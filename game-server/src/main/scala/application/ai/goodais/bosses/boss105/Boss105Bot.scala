package application.ai.goodais.bosses.boss105

import application.ai.goodais.GoodAIController
import gamelogic.abilities.boss.boss105.{Resonance, Reunion, Siphon, Skewer}
import gamelogic.buffs.abilities.classes.TriangleStunDebuff
import gamelogic.buffs.boss.boss105.{VolatileCharge, Yearning}
import gamelogic.entities.boss.boss105.{ReformationPoint, Shade}
import gamelogic.entities.boss.dawnoftime.Boss105
import gamelogic.entities.classes.{PlayerClass, Triangle}
import gamelogic.entities.{Entity, MovingBody, WithTarget}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

/** Behaviour shared by all the bots fighting [[Boss105]].
  *
  * Each bot has a place where it wants to be, and things to do once it is there. The place depends
  * on what is going on:
  *   - when the boss casts [[Skewer]], whoever is on the beam steps aside;
  *   - when a [[VolatileCharge]] is about to explode, its bearer goes away from everybody (unless
  *     it was dispelled in the meantime), and everybody else stays away from the bearer;
  *   - otherwise, each bot goes to its [[restingPosition]].
  *
  * Abilities that can be used while moving go in [[instantActions]]; the others (which need the bot
  * to be still) in [[actionsInPlace]].
  */
trait Boss105Bot[EntityType <: PlayerClass] extends GoodAIController[EntityType] {

  /** Where to be when nothing special is happening, and how close to it is close enough. */
  def restingPosition(gameState: GameState, me: EntityType, time: Long): (Complex, Double)

  /** What to do while moving, or once at the right position. */
  def instantActions(gameState: GameState, me: EntityType, time: Long): Vector[GameAction] =
    Vector.empty

  /** What to do once at the right position, while not moving. */
  def actionsInPlace(gameState: GameState, me: EntityType, time: Long): Vector[GameAction]

  /** Whether this bot keeps one of the twins in place. */
  def isTankOfATwin(gameState: GameState, me: EntityType): Boolean = false

  final protected def takeActions(
      gameState: GameState,
      me: EntityType,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      obstacleGraph: Graph
  ): Vector[GameAction] = {
    val (targetPosition, closeEnough) =
      maybeSpecialPosition(gameState, me, currentPosition, startTime, obstacleGraph)
        .getOrElse(restingPosition(gameState, me, startTime))

    val instant = instantActions(gameState, me, startTime)

    maybeMoveTowards(
      startTime,
      me,
      currentPosition,
      obstacleGraph.nearestLegalPosition(targetPosition),
      obstacleGraph,
      closeEnough
    ) match {
      case None =>
        if instant.nonEmpty then instant else actionsInPlace(gameState, me, startTime)
      case Some(moves) => moves ++ instant
    }
  }

  private def maybeSpecialPosition(
      gameState: GameState,
      me: EntityType,
      myPosition: Complex,
      time: Long,
      graph: Graph
  ): Option[(Complex, Double)] =
    Boss105Bot
      .maybeSkewerDodge(gameState, me.id, myPosition, time)
      .orElse(
        Boss105Bot.maybeChargeAvoidance(
          gameState,
          me.id,
          myPosition,
          time,
          graph,
          isTankOfATwin(gameState, me)
        )
      )

}

object Boss105Bot {

  /** Where the tank of the boss keeps it. */
  val bossTankPosition: Complex = Complex(-190, 0)

  /** Ranged players stand in the middle of the room, which is within reach of everything: of both
    * twins (their abilities have long ranges) and of both tanks (to be healed).
    */
  def rangedPosition(index: Int): Complex = Complex(0, if index % 2 == 0 then -80 else 80)

  inline def healerPosition: Complex = Complex(0, 80)

  /** Charges are given some time to be dispelled before their bearers and their neighbours do
    * something about them.
    */
  inline def chargeGracePeriod: Long = 3500L

  /** Safety margin added to the half-width of the beam by those who dodge it. */
  inline def beamMargin: Double = 25.0

  def theBoss(gameState: GameState): Option[Boss105] =
    gameState.bosses.values.collectFirst { case boss: Boss105 => boss }

  def theShade(gameState: GameState): Option[Shade] =
    gameState.allTEntities[Shade].values.headOption

  /** What the damage dealers hit: the Shade while it lives, the boss otherwise. */
  def focus(gameState: GameState): Option[MovingBody & WithTarget] =
    theShade(gameState).orElse(theBoss(gameState))

  /** The corner that the tank of the Shade should hold, when it is clear: the one where the Shade
    * will come back, or the one where it stands when it stays where it should. Nothing is clear
    * while the Shade is on its way to the boss.
    */
  def maybeShadeCorner(gameState: GameState, time: Long): Option[Complex] = {
    val corners = Shade.reformationCorners(Boss105.size)
    gameState
      .allTEntities[ReformationPoint]
      .values
      .headOption
      .map(_.pos)
      .orElse(
        theShade(gameState)
          .filterNot(shade => gameState.hasBuffOfType[Yearning](shade.id))
          .filter(shade =>
            theBoss(gameState).forall(
              _.currentPosition(time).distanceTo(shade.currentPosition(time)) > Boss105.size
            )
          )
          .map(shade => corners.minBy(_.distanceTo(shade.currentPosition(time)))(using ordering))
      )
  }

  /** The place where the tank of the Shade stands to hold the Shade in the given corner: a bit
    * towards the centre of the room so that the Shade stands right in the corner.
    */
  def shadeTankPosition(corner: Complex): Complex = corner + (-corner).safeNormalized * 70

  /** Whether the boss is casting [[Reunion]], or will be able to soon: the Triangle needs to keep
    * some energy for its stun.
    */
  def reunionIsNear(gameState: GameState, time: Long): Boolean =
    theBoss(gameState).exists { boss =>
      val casting = gameState.castingEntityInfo
        .get(boss.id)
        .exists(info => info.isCasting(time, 0L) && info.ability.isInstanceOf[Reunion])
      val remainingCooldown = boss.relevantUsedAbilities
        .get(Reunion.abilityId)
        .fold(0L)(lastUse => (lastUse.cooldown - (time - lastUse.time)) max 0L)
      casting || remainingCooldown < reunionAnticipation
    }

  /** How long before the boss can call the Shade the Triangle starts saving energy. */
  inline def reunionAnticipation: Long = 9000L

  /** The Shade must not be hit while the boss calls it (and a Triangle is around to stun it), or
    * the stun would break right away: not when the call is being cast, since bullets and casts take
    * some time, and not once the stun holds.
    */
  def holdFireOnShade(gameState: GameState, time: Long): Boolean =
    gameState.players.values.exists(_.isInstanceOf[Triangle]) && (shadeIsHeldByStun(gameState) ||
      shadeJustAnsweredTheCall(gameState, time) ||
      theBoss(gameState).exists(boss =>
        gameState.castingEntityInfo
          .get(boss.id)
          .exists(info => info.isCasting(time, 0L) && info.ability.isInstanceOf[Reunion])
      ))

  /** The stun needs a moment to land, and what has been fired at the Shade in the meantime would
    * break it.
    */
  private def shadeJustAnsweredTheCall(gameState: GameState, time: Long): Boolean =
    gameState.allTBuffs[Yearning].exists(buff => time - buff.appearanceTime < 2000L)

  def shadeIsYearning(gameState: GameState): Boolean =
    theShade(gameState).exists(shade => gameState.hasBuffOfType[Yearning](shade.id))

  /** A Shade which is stunned while the boss calls it stays where it is. Nobody should hit it, as
    * this would break the stun.
    */
  def shadeIsHeldByStun(gameState: GameState): Boolean =
    theShade(gameState).exists(shade =>
      gameState.hasBuffOfType[Yearning](shade.id) &&
        gameState.hasBuffOfType[TriangleStunDebuff](shade.id)
    )

  private val ordering: Ordering[Double] = Ordering.Double.TotalOrdering

  /** If the boss is casting [[Skewer]] and `myPosition` is too close to the beam, a position where
    * the beam is no danger.
    */
  def maybeSkewerDodge(
      gameState: GameState,
      myId: Entity.Id,
      myPosition: Complex,
      time: Long
  ): Option[(Complex, Double)] =
    for {
      boss   <- theBoss(gameState)
      info   <- gameState.castingEntityInfo.get(boss.id).filter(_.isCasting(time, 0))
      skewer <- Some(info.ability).collect { case skewer: Skewer => skewer }
      if skewer.targetId != myId
      target <- gameState.players.get(skewer.targetId)
      source    = boss.currentPosition(time)
      through   = target.currentPosition(time)
      direction = (through - source).safeNormalized
      relative  = myPosition - source
      if direction.modulus2 > 0 &&
        math.abs(relative.crossProduct(direction)) < Skewer.halfWidth + beamMargin &&
        relative.|*|(direction) > 0
    } yield {
      val side          = if relative.crossProduct(direction) < 0 then 1.0 else -1.0
      val perpendicular = direction.orthogonal * side
      val along         = relative.|*|(direction)
      (source + direction * along + perpendicular * (Skewer.halfWidth + beamMargin + 25)) -> 8.0
    }

  /** Bearers of a charge about to explode run away from everybody, and everybody else keeps away
    * from them.
    */
  def maybeChargeAvoidance(
      gameState: GameState,
      myId: Entity.Id,
      myPosition: Complex,
      time: Long,
      graph: Graph,
      holdsATwin: Boolean
  ): Option[(Complex, Double)] = {
    val ripeCharges = gameState
      .allTBuffs[VolatileCharge]
      .filter(charge => time - charge.appearanceTime > chargeGracePeriod)
      .toVector

    if ripeCharges.exists(_.bearerId == myId) then
      Some(isolatedPosition(gameState, myId, myPosition, time, graph, holdsATwin) -> 15.0)
    else
      ripeCharges
        .flatMap(charge => gameState.players.get(charge.bearerId))
        .map(bearer => bearer.currentPosition(time))
        .filter(_.distanceTo(myPosition) < VolatileCharge.radius + 35)
        .minByOption(_.distanceTo(myPosition))(using ordering)
        .map { bearerPosition =>
          val away      = (myPosition - bearerPosition).safeNormalized
          val direction = if away.modulus2 > 0 then away else Complex(0, 1)
          (bearerPosition + direction * (VolatileCharge.radius + 50)) -> 15.0
        }
  }

  /** The closest spot (on a grid covering the room) which is far enough from everybody else for the
    * explosion to hurt nobody. A tank, whose twin follows, also needs to stay far from the other
    * twin.
    */
  private def isolatedPosition(
      gameState: GameState,
      myId: Entity.Id,
      myPosition: Complex,
      time: Long,
      graph: Graph,
      holdsATwin: Boolean
  ): Complex = {
    val others =
      gameState.players.values.filterNot(_.id == myId).map(_.currentPosition(time)).toVector
    val twins = Vector(
      theBoss(gameState).map(_.currentPosition(time)),
      theShade(gameState).map(_.currentPosition(time))
    ).flatten
    // the twin that stands next to me is the one that follows me wherever I go
    val otherTwins =
      if holdsATwin then twins.sortBy(_.distanceTo(myPosition))(using ordering).drop(1)
      else Vector.empty

    def score(candidate: Complex): Double = {
      val clearance =
        others.map(_.distanceTo(candidate)).minOption.getOrElse(1000.0) min
          (VolatileCharge.radius + 40)
      val resonance =
        if otherTwins.exists(_.distanceTo(candidate) < Resonance.range + 80) then 1000.0 else 0.0
      clearance - 0.3 * candidate.distanceTo(myPosition) - resonance
    }

    val limit = Boss105.size - 60
    val grid = for {
      x <- (-limit.toInt to limit.toInt by 60).toVector
      y <- (-limit.toInt to limit.toInt by 60).toVector
      candidate = Complex(x.toDouble, y.toDouble)
      if graph.isLegal(candidate)
    } yield candidate

    grid.maxByOption(score)(using ordering).getOrElse(myPosition)
  }

  /** Whether the player is one of the bots (as opposed to a human). */
  def playerClassIsBot(player: PlayerClass): Boolean =
    player.cls.parsePlayerName(player.name).isDefined

  def siphonIsBeingCast(gameState: GameState, shade: Shade, time: Long): Option[Siphon] =
    gameState.castingEntityInfo
      .get(shade.id)
      .filter(_.isCasting(time, 0))
      .map(_.ability)
      .collect { case siphon: Siphon => siphon }

}
