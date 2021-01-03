package game

import assets.Asset
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.Owner
import com.raquo.airstream.signal.{Signal, SignalViewer}
import game.ui.GameDrawer
import game.ui.effects.{ChoosingAbilityPositionEffect, EffectsManager}
import game.ui.gui.ReactiveGUIDrawer
import game.ui.reactivepixi.ReactiveStage
import game.ui.effects.targetmanager.TargetManager
import gamelogic.abilities.Ability
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.Boss101
import gamelogic.entities.{Entity, LivingEntity, MovingBody}
import gamelogic.gamestate.gameactions.MovingBodyMoves
import gamelogic.gamestate.{AddAndRemoveActions, GameAction, GameState, ImmutableActionCollector}
import gamelogic.physics.Complex
import models.bff.ingame.{InGameWSProtocol, UserInput}
import typings.pixiJs.PIXI.LoaderResource
import typings.pixiJs.mod.{Application, Container}
import utils.pixi.monkeypatching.PIXIPatching._
import assets.Asset.ingame.gui.bars.{liteStepBar, _}
import game.ui.effects.soundeffects.SoundEffectsManager
import assets.sounds.SoundAsset
import typings.std.global.Audio

import scala.Ordering.Double.TotalOrdering
import scala.scalajs.js.timers.setTimeout
import game.ui.effects.errormessages.ErrorMessagesManager

final class GameStateManager(
    reactiveStage: ReactiveStage,
    initialGameState: GameState,
    $actionsFromServer: EventStream[AddAndRemoveActions],
    socketOutWriter: Observer[InGameWSProtocol.Outgoing],
    userControls: UserControls,
    playerId: Entity.Id,
    bossStartingPosition: Complex,
    deltaTimeWithServer: Long,
    resources: PartialFunction[Asset, LoaderResource],
    soundResources: PartialFunction[SoundAsset[_], Audio],
    maybeTargetWriter: Observer[Option[Entity]]
)(implicit owner: Owner) {

  @inline def application: Application = reactiveStage.application

  val slowSocketOutWriterBus = new EventBus[InGameWSProtocol.Outgoing]
  slowSocketOutWriterBus.events.throttle(100).foreach(socketOutWriter.onNext)

  private val gameStateUpdatesBus                      = new EventBus[(GameState, Long)]
  val gameStateUpdates: EventStream[(GameState, Long)] = gameStateUpdatesBus.events

  def serverTime: Long = System.currentTimeMillis() + deltaTimeWithServer

  private var actionCollector = ImmutableActionCollector(initialGameState)
  private val gameDrawer = new GameDrawer(
    reactiveStage,
    resources,
    bossStartingPosition,
    socketOutWriter.contramap[Unit](_ => InGameWSProtocol.LetsBegin)
  )

  private val gameStateBus: EventBus[GameState] = new EventBus[GameState]

  val $gameStates: Signal[GameState] = gameStateBus.events.startWith(initialGameState)
  private val $strictGameStates      = $gameStates.observe

  /**
    * Signal giving at all time the game position of the user mouse.
    */
  val $gameMousePosition: SignalViewer[Complex] =
    userControls.$effectiveMousePosition.map(gameDrawer.camera.mousePosToWorld)
      .startWith(Complex.zero)
      .observe
  val $mouseAngleWithPosition: SignalViewer[Angle] = $gameMousePosition.map { mousePosition =>
    val myPositionNow = $strictGameStates.now().players.get(playerId).fold(Complex.zero)(_.pos)
    (mousePosition - myPositionNow).arg
  }.observe

  val targetFromGUIBus = new EventBus[Entity.Id]

  /**
    * Signal containing the current target of the user, starting with no target.
    *
    * This target only has meaning inside the GUI (it is not encoded in the game state). However, it used when using
    * abilities involving targeting something.
    */
  val $maybeTarget: Signal[Option[MovingBody with LivingEntity]] =
    EventStream
      .merge(
        targetFromGUIBus.events.withCurrentValueOf($gameStates).map {
          case (id, gameState) => gameState.livingEntityAndMovingBodyById(id)
        },
        reactiveStage.clickEventsWorldPositions.withCurrentValueOf($gameStates).map {
          case (mousePosition, state) =>
            state.allTargetableEntities.toList
              .sortBy(_.shape.radius) // sorting by radius so that smaller entities are clicked in higher priority
              .find(entity => entity.shape.contains(mousePosition, entity.pos, entity.rotation))
        }
      )
      .startWith(Option.empty[MovingBody with LivingEntity])

  /** There is a side effect in the constructor here. */
  new NextTargetHandler(
    playerId,
    userControls.downInputs.collect { case UserInput.NextTarget => UserInput.NextTarget },
    $gameStates,
    targetFromGUIBus.writer,
    () => serverTime
  )

  $maybeTarget.foreach(maybeTargetWriter.onNext)

  private var unconfirmedActions: List[GameAction] = Nil

  private def nextGameState(): Unit =
    gameStateBus.writer.onNext(actionCollector.currentGameState.applyActions(unconfirmedActions))

  $actionsFromServer.foreach {
    case AddAndRemoveActions(actionsToAdd, oldestTimeToRemove, idsOfActionsToRemove) =>
      actionCollector = actionCollector.slaveAddAndRemoveActions(actionsToAdd, oldestTimeToRemove, idsOfActionsToRemove)

      unconfirmedActions = unconfirmedActions.dropWhile(_.time < actionCollector.currentGameState.time)

      setTimeout(1) {
        actionsToAdd.filterNot(action => idsOfActionsToRemove.contains(action.id)).foreach(newActionsBus.writer.onNext)
      }

      nextGameState()
  }

  private val newActionsBus: EventBus[GameAction] = new EventBus[GameAction]

  val $actionsWithStates: EventStream[(GameAction, GameState)] = newActionsBus.events.withCurrentValueOf($gameStates)

  val effectsManager = new EffectsManager(playerId, $actionsWithStates, gameDrawer.camera, application, resources)
  val soundEffectsManager = new SoundEffectsManager(
    playerId,
    $actionsWithStates,
    soundResources
  )

  val pressedUserInputSignal: SignalViewer[Set[UserInput]] = userControls.$pressedUserInput.observe

  val choosingAbilityEffectPositionBus = new EventBus[Option[Ability.AbilityId]]
  val isChoosingAbilityEffectPosition: Signal[Option[Ability.AbilityId]] =
    choosingAbilityEffectPositionBus.events.startWith(None)

  val useAbilityBus = new EventBus[Ability.AbilityId]

  /** There is a side effect in the constructor. */
  new CastAbilitiesHandler(
    playerId,
    userControls,
    $gameStates,
    $maybeTarget,
    $gameMousePosition,
    socketOutWriter,
    choosingAbilityEffectPositionBus.writer,
    isChoosingAbilityEffectPosition,
    useAbilityBus.events,
    gameDrawer,
    deltaTimeWithServer: Long,
    () => serverTime
  )

  new MarkerManager(userControls, $maybeTarget, $gameMousePosition, socketOutWriter, () => serverTime)

  /** Side effect-full constructor */
  new ChoosingAbilityPositionEffect(
    application,
    new Container,
    gameDrawer.camera,
    $gameMousePosition.combineWith(isChoosingAbilityEffectPosition).changes
  )

  /** After [[game.ui.GameDrawer]] so that gui is on top of the game. */
  val guiDrawer = new ReactiveGUIDrawer(
    playerId,
    reactiveStage,
    resources,
    targetFromGUIBus.writer,
    $maybeTarget,
    useAbilityBus.writer,
    gameStateUpdates
  )

  new TargetManager(
    gameDrawer,
    $maybeTarget,
    gameStateUpdates,
    guiDrawer.guiContainer,
    resources(minimalistBar).texture,
    resources(minimalistBar).texture,
    gameDrawer.camera
  )

  var lastTimeStamp = org.scalajs.dom.window.performance.now()

  private val ticker = (_: Double) => {
    val pressedUserInput = pressedUserInputSignal.now()

    val playerMovement = UserInput.movingDirection(pressedUserInput)

    val now       = serverTime
    val gameState = $strictGameStates.now()
    val deltaTime = now - lastTimeStamp
    lastTimeStamp = now.toDouble

    ErrorMessagesManager.updateMessageView(now)

    gameState.players.get(playerId) match {
      case Some(entity) =>
        val nextPos = entity.lastValidPosition(
          entity.pos + entity.speed * playerMovement * deltaTime / 1000,
          gameState.obstaclesLike.toList
        )
        val moving   = playerMovement != Complex.zero
        val rotation = $mouseAngleWithPosition.now()

        if (moving || entity.moving != moving || rotation != entity.rotation) {
          val newAction = MovingBodyMoves(
            0L,
            now,
            playerId,
            nextPos,
            entity.direction,
            rotation,
            entity.speed,
            moving
          )
          unconfirmedActions = unconfirmedActions :+ newAction

          socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(List(newAction)))

          nextGameState()
        }
      case None =>
      // do nothing, player is dead
    }

    val gameStateToDraw = $strictGameStates.now()
    gameDrawer.drawGameState(
      gameStateToDraw,
      gameStateToDraw.players
        .get(playerId)
        .map(_.pos)
        .orElse {
          gameStateToDraw.bosses.headOption.map(_._2.currentPosition(now)).map { targetCameraPosition =>
            val currentCameraPosition = gameDrawer.camera.worldCenter
            val distance              = targetCameraPosition distanceTo currentCameraPosition
            val cameraMovementSize    = Boss101.fullSpeed * 0.5 * deltaTime / 1000
            if (distance < cameraMovementSize) targetCameraPosition
            else
              currentCameraPosition + (targetCameraPosition - currentCameraPosition).safeNormalized * cameraMovementSize
          }
        }
        .getOrElse(Complex.zero),
      now
    )
    effectsManager.update(now, gameState)

    gameStateUpdatesBus.writer.onNext((gameState, now))

  }

  application.ticker.add(ticker)

}
