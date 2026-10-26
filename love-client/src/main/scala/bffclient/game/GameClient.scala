package bffclient.game

import java.nio.ByteBuffer

import bffclient.love.{Event, LoveInput}
import bffclient.lua.{ScalaLove, WebSocket}
import bffclient.render.{Effects, Smoothing}
import boopickle.Default.*
import communication.BFFPicklers.*
import gamelogic.abilities.Ability
import gamelogic.entities.{Body, Entity, LivingEntity}
import gamelogic.gameextras.{GameMarker, GameMarkerInfo}
import gamelogic.gamestate.gameactions.markers.{RemoveMarker, UpdateMarker}
import gamelogic.gamestate.{ActionGatherer, GameAction, GameState, GreedyActionGatherer}
import gamelogic.gamestate.gameactions.{EndGame, MovingBodyMoves}
import gamelogic.physics.Complex
import models.bff.ingame.{Controls, InGameWSProtocol, UserInput}
import models.bff.ingame.InGameWSProtocol.*

/** Connects to a real Battle for Flatland game server, plays the start-of-game handshake like the
  * frontend, keeps a client-side copy of the game state from the server's updates (with our own
  * movements predicted), and sends the player's inputs.
  */
final class GameClient(
    ws: WebSocket,
    user: String,
    headless: Boolean,
    val controls: Controls,
    val controlsInfo: String,
    mouseWorld: () => Complex,
    /** The entity whose frame (in the HUD) is under the mouse. */
    hovered: () => Option[Entity.Id],
    /** Simulated round trip latency in seconds (BFF_LAG_MS), half on each way, to try the
      * prediction.
      */
    lag: Double = 0
) {
  private val incoming = scala.collection.mutable.Queue.empty[(Double, String)]
  private val outgoing = scala.collection.mutable.Queue.empty[(Double, String)]

  private var gatherer: ActionGatherer    = GreedyActionGatherer(GameState.empty)
  var myId: Option[Entity.Id]             = None
  var messagesReceived                    = 0
  var roundTrip                           = -1L
  private var elapsed                     = 0.0
  private var lastStatus                  = 0.0
  private var pingCount                   = 0
  private var actionsReceived             = 0
  private var bytesReceived               = 0L
  private var clockOffset                 = 0L
  /** When the boss spawned (our `elapsed`). */
  private var gameBeganAt: Option[Double] = None

  /** Every player is ready: they can move, until one of them starts the game. */
  private var everyoneReady = false

  /** Where the boss spawns, and where the "Start game" button is (like in the frontend). */
  var bossStartingPosition = Option.empty[Complex]

  /** BFF has no countdown before the boss spawns (the game server spawns it when a player sends
    * `LetsBegin`). Its start is shared with the other players as a Star marker fixed exactly on the
    * boss's starting position (which nobody can place by hand), and the time of its action, on the
    * server's clock, is the start of the countdown. At its end, every client sends `LetsBegin`
    * (once is enough, the others are ignored), and the one which started it removes the marker.
    */
  private val countdownMarker    = GameMarker.Star
  val countdownDuration          = 5000L
  private var countdownRequested = false
  private var countdownStartedAt = Option.empty[Long]
  private var letsBeginSent      = false

  private var readyToStartSent = false
  private var scriptedMovements           = 0
  private var scriptedDirection           = Complex.zero

  /** Whether the player faces its target (instead of the mouse), toggled by `ToggleTargetLockIn`.
    */
  var lockInToTarget = false

  // Abilities and targeting
  private var caster: Option[AbilityCaster]       = None
  private var targetId: Option[Entity.Id]         = None
  var choosingPosition: Option[Ability.AbilityId] = None
  private var lastError: Option[(String, Double)] = None
  private var castsSent                           = 0
  private val recentTargets = scala.collection.mutable.Map.empty[Entity.Id, Double]

  /** Our last movement, shown before the server echoes it (the frontend's "unconfirmed actions").
    */
  private var predictedMovement: Option[MovingBodyMoves] = None
  private var lastRotationSent: Option[Double]           = None
  private var lastMovementSentAt                         = 0L
  private var projectedState: GameState                  = GameState.empty
  private var lastScriptedCast                           = 0.0

  /** The message of the last refused cast, for 2 seconds. */
  def errorMessage: Option[String] = lastError.collect {
    case (message, at) if elapsed - at < 2 => message
  }

  /** Living entities that can be targeted, bosses first. */
  def targetableEntities: List[LivingEntity & Body] =
    gameState.entities.values.toList
      .collect { case e: (LivingEntity & Body) => e }
      .sortBy(e => (if gameState.bosses.contains(e.id) then 0 else 1, e.id.toString))

  /** The selected target if it still exists, otherwise the boss. */
  /** Targets this entity (e.g. its frame was clicked). */
  def selectTarget(id: Entity.Id): Unit = targetId = Some(id)

  def currentTarget: Option[Entity.Id] =
    targetId.filter(gameState.entities.contains).orElse(gameState.bosses.keys.headOption)

  private def request(castRequest: CastRequest): Unit = castRequest match {
    case CastRequest.Send(action) =>
      send(GameActionWrapper(Vector(action)))
      castsSent += 1
    case CastRequest.ChoosePosition(abilityId) =>
      choosingPosition = Some(abilityId)
    case CastRequest.Error(message) =>
      lastError = Some((message, elapsed))
      if headless then println(s"Cast refused: $message")
  }

  /** Escape is not part of the controls: it cancels the zone placement, or opens the game menu (or
    * quits, when headless).
    */
  def keyPressed(scancode: String): Unit =
    if scancode == "escape" then {
      if choosingPosition.isDefined then choosingPosition = None
      else if headless then quit()
      else menuOpen = true
    }

  /** Like the frontend, inputs are triggered when keys are released. Movement is polled every
    * frame.
    */
  def keyReleased(scancode: String): Unit =
    LoveInput.inputCode(scancode).map(controls.getOrUnknown).foreach(handleInput)

  /** Mouse buttons can be bound in the controls too (`button` is LÖVE's number). */
  def mouseButtonReleased(button: Int): Unit = handleInput(
    controls.getOrUnknown(LoveInput.mouseCode(button))
  )

  def handleInput(input: UserInput): Unit = input match {
    case abilityInput: UserInput.AbilityInput =>
      choosingPosition = None
      for
        c  <- caster
        id <- myId
        me <- gameState.players.get(id)
      do abilityInput.abilityId(me) match {
        case Some(abilityId) =>
          request(
            c.cast(
              abilityId,
              me,
              gameState,
              serverNow,
              currentTarget,
              mouseWorld(),
              if controls.mouseOverEnabled then hovered() else None
            )
          )
        case None =>
          request(CastRequest.Error(s"No ability on input ${abilityInput.abilityIndex + 1}"))
      }
    case UserInput.NextTarget =>
      nextTarget()
    case UserInput.ToggleTargetLockIn =>
      lockInToTarget = !lockInToTarget
    case UserInput.GameMarkerInput(marker, onTarget) =>
      val info =
        if onTarget then currentTarget.map(GameMarkerInfo.GameMarkerOnEntity(marker, _))
        else Some(GameMarkerInfo.FixedGameMarker(marker, mouseWorld()))
      info.foreach(i =>
        send(GameActionWrapper(Vector(UpdateMarker(GameAction.Id.dummy, serverNow, i))))
      )
    case _ =>
      () // directions are polled every frame
  }

  /** Like the frontend's `NextTargetHandler`: the closest mob not chosen in the last 5 seconds. */
  private def nextTarget(): Unit =
    for
      id <- myId
      me <- gameState.players.get(id)
    do {
      val candidates =
        gameState.allTargetableEntities.filter(_.teamId == Entity.teams.mobTeam).toList
      val ids = candidates.map(_.id).toSet
      recentTargets.filterInPlace((targetId, _) => ids.contains(targetId))
      val tooRecent = recentTargets.collect {
        case (targetId, at) if elapsed - at <= 5 => targetId
      }.toSet
      val next = candidates
        .filterNot(e => tooRecent.contains(e.id))
        .minByOption(_.pos.distanceTo(me.pos))
        .map(_.id)
        .orElse(recentTargets.minByOption(_._2).map(_._1))
      next.foreach { chosen =>
        recentTargets(chosen) = elapsed
        targetId = Some(chosen)
      }
    }

  /** Left click: places the zone being chosen, or selects the entity under the mouse. */
  def clicked(world: Complex): Unit = choosingPosition match {
    case Some(abilityId) =>
      choosingPosition = None
      caster.foreach(c => request(c.castAtPosition(abilityId, world, gameState, serverNow)))
    case None =>
      val now = serverNow
      targetableEntities
        .find(e => e.shape.contains(world, e.currentPosition(now), e.rotation))
        .foreach(e => targetId = Some(e.id))
  }

  /** Players are placing themselves, before the boss spawns. */
  def inPreGame: Boolean = everyoneReady && !gameState.started && !finished

  /** Milliseconds before the boss spawns, once someone started the countdown. */
  def countdownRemaining: Option[Long] =
    countdownStartedAt
      .filter(_ => inPreGame)
      .map(start => math.max(0L, start + countdownDuration - serverNow))

  /** The "Start game" button was clicked. */
  def startCountdown(): Unit =
    if inPreGame && !countdownRequested && countdownStartedAt.isEmpty then
      for position <- bossStartingPosition do {
        countdownRequested = true
        val signal = GameMarkerInfo.FixedGameMarker(countdownMarker, position)
        send(GameActionWrapper(Vector(UpdateMarker(GameAction.Id.dummy, serverNow, signal))))
      }

  /** The marker which shares the start of the countdown (not drawn). */
  def isCountdownSignal(info: GameMarkerInfo): Boolean = info match {
    case GameMarkerInfo.FixedGameMarker(marker, position) =>
      marker == countdownMarker && bossStartingPosition.contains(position)
    case _ => false
  }

  /** Sends `LetsBegin` at the end of the countdown, and notes when the boss spawned. */
  private def preGame(): Unit = {
    if headless && inPreGame then startCountdown() // the scripted runs start right away
    if countdownRemaining.contains(0L) && !letsBeginSent then {
      letsBeginSent = true
      println("-> LetsBegin (end of the countdown)")
      send(LetsBegin)
    }
    if gameState.started && gameBeganAt.isEmpty then {
      gameBeganAt = Some(elapsed)
      if countdownRequested then
        send(GameActionWrapper(Vector(RemoveMarker(GameAction.Id.dummy, serverNow, countdownMarker))))
    }
  }

  send(Ready(user))

  /** The game state from the server, with our predicted movement: what we show and play with. */
  def gameState: GameState = projectedState

  /** The game state from the server only. */
  def serverGameState: GameState = gatherer.currentGameState

  private def project(): Unit =
    projectedState = gatherer.currentGameState.applyActions(predictedMovement.toVector)

  /** Smoothed positions of the entities moved by the server, for drawing. */
  val smoothing = new Smoothing

  /** Visual effects of the actions (damage numbers, ...). */
  val effects = new Effects

  // Headless: the largest jumps and turns of the boss between two frames, actual and smoothed
  private var previousBoss: Option[(Complex, Complex, Double, Double)] = None
  private var largestBossJumps                                         = (0.0, 0.0)
  private var largestBossTurns                                         = (0.0, 0.0)

  private def measureBossJumps(): Unit =
    for boss <- gameState.bosses.values.headOption do {
      val now     = serverNow
      val current = (
        boss.currentPosition(now),
        smoothing.position(boss, now),
        boss.rotation.value,
        smoothing.rotation(boss).value
      )
      for (raw, shown, rawRotation, shownRotation) <- previousBoss do {
        def turn(from: Double, to: Double) = math.abs(Smoothing.shortestAngle(to - from))
        largestBossJumps = (
          math.max(largestBossJumps._1, (current._1 - raw).modulus),
          math.max(largestBossJumps._2, (current._2 - shown).modulus)
        )
        largestBossTurns = (
          math.max(largestBossTurns._1, turn(rawRotation, current._3)),
          math.max(largestBossTurns._2, turn(shownRotation, current._4))
        )
      }
      previousBoss = Some(current)
    }

  /** Estimate of the server's clock (from the ping/pong exchanges), used to interpolate positions.
    */
  def serverNow: Long = System.currentTimeMillis() + clockOffset

  private def send(message: InGameWSProtocol): Unit = {
    val buffer = Pickle.intoBytes[InGameWSProtocol](message)
    val bytes  = new Array[Byte](buffer.remaining())
    buffer.get(bytes)
    if lag > 0 then outgoing.enqueue((elapsed + lag / 2, ScalaLove.bytesToString(bytes)))
    else ws.send(ScalaLove.bytesToString(bytes))
  }

  def quit(): Unit = {
    ws.close()
    Event.quit(0)
  }

  /** The game ended, or the game server closed the connection. */
  def finished: Boolean = gameState.ended || !ws.isOpen()

  /** The game menu (opened with Escape). The game goes on, but the player stops moving. */
  var menuOpen = false

  /** The human players of the game (the AIs are not included). */
  private var humanPlayers = Vector.empty[String]

  /** How leaving the game now would end it. */
  def exit: GameExit =
    if gameState.ended then GameExit.Ended
    else if !ws.isOpen() then GameExit.ConnectionLost
    else GameExit.LeftEarly(onlyHuman = humanPlayers.forall(_ == user))

  /** Closes the connection to the game server (e.g. to go back to the menus). */
  def leave(): Unit = ws.close()

  /** During the fight. */
  def canSurrender: Boolean = gameState.started && !gameState.ended && ws.isOpen()

  /** Ends the fight, for everyone: BFF has no surrender, but the game server takes the players'
    * actions as they are, and the game's own `EndGame` ends it (in a defeat, since the boss is alive).
    * The end of the game then arrives like any other, with its overlay.
    */
  def surrender(): Unit = {
    send(GameActionWrapper(Vector(EndGame(GameAction.Id.dummy, serverNow))))
    menuOpen = false
  }

  def update(dt: Double): Unit = {
    elapsed += dt

    var data = ws.poll()
    while data != null do {
      incoming.enqueue((elapsed + lag / 2, data))
      data = ws.poll()
    }
    while incoming.nonEmpty && incoming.head._1 <= elapsed do {
      val (_, message) = incoming.dequeue()
      bytesReceived += message.length
      messagesReceived += 1
      handle(
        Unpickle[InGameWSProtocol].fromBytes(ByteBuffer.wrap(ScalaLove.stringToBytes(message)))
      )
    }
    while outgoing.nonEmpty && outgoing.head._1 <= elapsed do ws.send(outgoing.dequeue()._2)

    if elapsed - lastStatus >= 1.0 then {
      lastStatus = elapsed
      pingCount += 1
      send(Ping(System.currentTimeMillis(), s"ping-$pingCount"))
      if headless then println(status)
    }

    preGame()
    project()
    if everyoneReady && !finished then {
      if headless then { scriptedMovement(dt); scriptedCasts() }
      else keyboardMovement(dt)
      project()
    }
    smoothing.update(gameState, serverNow, dt, myId)
    if headless then measureBossJumps()

    if headless && (gameState.ended || elapsed > 60 || !ws.isOpen()) then {
      println(status)
      println(
        s"Finished: game ended=${gameState.ended}, players won=${gameState.playersWon}, " +
          s"socket open=${ws.isOpen()}, $messagesReceived messages ($bytesReceived bytes), $actionsReceived actions, " +
          s"$castsSent casts sent, largest boss jump between frames: " +
          f"${largestBossJumps._1}%.1f (smoothed: ${largestBossJumps._2}%.1f), " +
          f"largest boss turn between frames: ${largestBossTurns._1}%.2f rad (smoothed: ${largestBossTurns._2}%.2f)"
      )
      quit()
    }
  }

  /** Movement from the held direction controls, facing the mouse (or the target when locked in),
    * like the frontend.
    */
  private def keyboardMovement(dt: Double): Unit =
    for
      id <- myId
      me <- gameState.players.get(id)
    do {
      val directionKeys = Map[UserInput, Controls.InputCode](
        UserInput.Up    -> controls.upKey,
        UserInput.Down  -> controls.downKey,
        UserInput.Left  -> controls.leftKey,
        UserInput.Right -> controls.rightKey
      )
      val held = directionKeys.collect {
        case (input, code) if !menuOpen && LoveInput.isDown(code) => input
      }.toSet
      val now      = serverNow
      val position = me.currentPosition(now)
      val facing   = (for
        targetId <- currentTarget if lockInToTarget
        case target: Body <- gameState.entities.get(targetId)
      yield target.currentPosition(now) - position).getOrElse(mouseWorld() - position)
      move(UserInput.movingDirection(held), facing, dt)
    }

  /** Moves our player for this frame, like the frontend: the new position is computed here (with
    * the obstacles), sent to the server, and shown right away on top of the server's game state,
    * without waiting for the server to echo it (one round trip later).
    */
  private def move(direction: Complex, facing: Complex, dt: Double): Unit =
    for
      id <- myId
      me <- gameState.players.get(id)
    do {
      val now       = serverNow
      val moving    = direction != Complex.zero
      val obstacles = gameState.obstacles.values
      val position  =
        if moving then {
          // At most 100 ms, so that a long frame does not make us jump
          val step   = me.speed * math.min(dt, 0.1) * Complex.rotation(direction.arg)
          val target = me.pos + step // our previous frame (predicted) position
          // BFF's lastValidPosition ignores moves under 2 units, which are common at high frame rates
          if step.modulus >= 2 then me.lastValidPosition(target, obstacles)
          else if
            obstacles
              .exists(o => o.shape.collides(o.pos, o.rotation, me.shape, target, me.rotation))
          then me.pos
          else target
        } else me.currentPosition(now, obstacles)
      val action = MovingBodyMoves(
        GameAction.Id.zero,
        now,
        id,
        position,
        if moving then direction.arg else me.direction,
        facing.arg,
        me.speed,
        moving
      )
      // Sent when starting, stopping, turning or changing direction, and about 60 times per second
      // while moving (the rotation is rounded, so that tiny mouse moves do not flood the server)
      val rotation         = math.round(facing.arg.value * 50) / 50.0
      val directionChanged = moving && me.direction != action.direction
      if
        moving != me.moving || directionChanged || !lastRotationSent.contains(rotation) ||
        (moving && now - lastMovementSentAt >= 16)
      then {
        lastRotationSent = Some(rotation)
        lastMovementSentAt = now
        send(GameActionWrapper(Vector(action)))
      }
      predictedMovement = Some(action)
    }

  /** Two seconds after the start, walk towards the boss for one second. */
  private def scriptedMovement(dt: Double): Unit =
    for
      began <- gameBeganAt
      boss  <- gameState.bosses.values.headOption
      id    <- myId
      me    <- gameState.players.get(id)
    do {
      val towardsBoss = boss.currentPosition(serverNow) - me.currentPosition(serverNow)
      val walking     = elapsed - began > 2 && elapsed - began <= 3
      if walking && scriptedMovements == 0 then {
        println(s"Walking towards the boss from ${me.currentPosition(serverNow)}")
        scriptedDirection = towardsBoss.safeNormalized
        scriptedMovements += 1
      } else if !walking && scriptedMovements == 1 then {
        val server = serverGameState.players.get(id).map(_.currentPosition(serverNow))
        println(s"Stopping at ${me.currentPosition(serverNow)} (server: ${server.getOrElse("-")})")
        scriptedMovements += 1
      }
      move(if walking then scriptedDirection else Complex.zero, towardsBoss, dt)
    }

  /** Once at the boss, hit it (the triangle's Direct Hit) every half second. */
  private def scriptedCasts(): Unit =
    for
      c  <- caster
      id <- myId
      me <- gameState.players.get(id)
    do
      if
        scriptedMovements == 2 && elapsed - lastScriptedCast > 0.5 && !gameState.entityIsCasting(id)
      then {
        lastScriptedCast = elapsed
        request(
          c.cast(Ability.triangleDirectHit, me, gameState, serverNow, currentTarget, Complex.zero)
        )
      }

  private def status: String = {
    val bosses =
      gameState.bosses.values.map(b => s"${b.name} life=${math.round(b.life)}").mkString(", ")
    def show(z: Complex) = s"(${math.round(z.re)}, ${math.round(z.im)})"
    val me               = myId.flatMap(gameState.players.get).fold("-") { p =>
      val server =
        myId.flatMap(serverGameState.players.get).fold("-")(s => show(s.currentPosition(serverNow)))
      s"life=${math.round(p.life)} pos=${show(p.currentPosition(serverNow))} (server: $server)"
    }
    s"[t=${math.round(elapsed * 10) / 10.0}s] messages=$messagesReceived actions=$actionsReceived entities=${gameState.entities.size} " +
      s"boss: [$bosses] me: $me rtt=${roundTrip}ms started=${gameState.started} ended=${gameState.ended}"
  }

  /** Like the frontend (once its assets are loaded), after receiving our entity id and the boss's
    * position: they come from the game master, which only exists once every human player is ready,
    * and which ignores a `ReadyToStart` received before.
    */
  private def readyToStart(): Unit =
    if myId.isDefined && bossStartingPosition.isDefined && !readyToStartSent then {
      readyToStartSent = true
      send(ReadyToStart(user))
    }

  private def handle(message: InGameWSProtocol): Unit = message match {
    case AllPlayers(names) =>
      println(s"<- AllPlayers(${names.mkString(", ")})")
      humanPlayers = names.toVector
    case BuddyIsReady(name) =>
      println(s"<- BuddyIsReady($name)")
    case EveryoneIsReady =>
      println("<- EveryoneIsReady")
      everyoneReady = true
    case YourEntityIdIs(id) =>
      println(s"<- YourEntityIdIs($id)")
      myId = Some(id)
      caster = Some(new AbilityCaster(id))
      readyToStart()
    case StartingBossPosition(x, y) =>
      println(s"<- StartingBossPosition($x, $y)")
      bossStartingPosition = Some(Complex(x, y))
      readyToStart()
    case AddAndRemoveActions(toAdd, oldestTimeToRemove, idsToRemove) =>
      actionsReceived += toAdd.length
      gatherer = gatherer.slaveAddAndRemoveActions(toAdd, oldestTimeToRemove, idsToRemove)
      if countdownStartedAt.isEmpty then
        countdownStartedAt = toAdd.collectFirst {
          case UpdateMarker(_, time, info) if isCountdownSignal(info) => time
        }
      effects.received(toAdd, gatherer.currentGameState, myId)
    case RemoveActions(oldestTime, ids) =>
      gatherer = gatherer.slaveAddAndRemoveActions(Vector.empty, oldestTime, ids)
    case Pong(originalSendingTime, midwayDistantTime, _) =>
      val now = System.currentTimeMillis()
      roundTrip = now - originalSendingTime
      clockOffset = midwayDistantTime - (originalSendingTime + now) / 2
    case HeartBeat =>
      ()
    case other =>
      println(s"<- unexpected $other")
  }
}
