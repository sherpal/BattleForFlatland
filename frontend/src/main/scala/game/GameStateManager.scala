package game

import assets.Asset
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.Owner
import com.raquo.airstream.signal.{Signal, SignalViewer}
import game.ui.GameDrawer
import game.ui.gui.GUIDrawer
import gamelogic.abilities.SimpleBullet
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.gamestate.gameactions.{DummyEntityMoves, EntityStartsCasting, MovingBodyMoves}
import gamelogic.gamestate.{AddAndRemoveActions, GameAction, GameState, ImmutableActionCollector}
import gamelogic.physics.Complex
import models.bff.ingame.{InGameWSProtocol, UserInput}
import org.scalajs.dom
import typings.pixiJs.mod.Application
import typings.pixiJs.PIXI.LoaderResource
import utils.pixi.monkeypatching.PIXIPatching._

final class GameStateManager(
    application: Application,
    initialGameState: GameState,
    $actionsFromServer: EventStream[AddAndRemoveActions],
    socketOutWriter: Observer[InGameWSProtocol.Outgoing],
    keyboard: Keyboard,
    mouse: Mouse,
    playerId: Entity.Id,
    deltaTimeWithServer: Long,
    resources: PartialFunction[Asset, LoaderResource]
)(implicit owner: Owner) {

  private var actionCollector = ImmutableActionCollector(initialGameState)
  private val gameDrawer      = new GameDrawer(application)

  /** After [[game.ui.GameDrawer]] so that gui is on top of the game. */
  private val guiDrawer = new GUIDrawer(playerId, application, resources)

  private val gameStateBus: EventBus[GameState] = new EventBus[GameState]

  val $gameStates: Signal[GameState] = gameStateBus.events.startWith(initialGameState)
  private val $strictGameStates      = $gameStates.observe

  val $gameMousePosition: SignalViewer[Complex] = mouse.$effectiveMousePosition.map(gameDrawer.camera.mousePosToWorld)
    .startWith(Complex.zero)
    .observe
  val $mouseAngleWithPosition: SignalViewer[Angle] = $gameMousePosition.map { mousePosition =>
    val myPositionNow = $strictGameStates.now.players.get(playerId).fold(Complex.zero)(_.pos)
    (mousePosition - myPositionNow).arg
  }.observe

  private var unconfirmedActions: List[GameAction] = Nil

  private def nextGameState(): Unit =
    gameStateBus.writer.onNext(actionCollector.currentGameState.applyActions(unconfirmedActions))

  $actionsFromServer.foreach {
    case AddAndRemoveActions(actionsToAdd, oldestTimeToRemove, idsOfActionsToRemove) =>
      actionCollector = actionCollector.slaveAddAndRemoveActions(actionsToAdd, oldestTimeToRemove, idsOfActionsToRemove)

      unconfirmedActions = unconfirmedActions.dropWhile(_.time < actionCollector.currentGameState.time)

      nextGameState()
  }

  val pressedUserInputSignal: SignalViewer[Set[UserInput]] = keyboard.$pressedUserInput.observe

  keyboard.$downKeyEvents.filter(_.code == "KeyE").foreach { _ =>
    val direction = $mouseAngleWithPosition.now

    val ability = new SimpleBullet(
      0L,
      System.currentTimeMillis,
      playerId,
      $strictGameStates.now.players.get(playerId).map(_.pos).getOrElse(Complex.zero),
      direction
    )

    val action = EntityStartsCasting(
      0L,
      System.currentTimeMillis,
      ability.castingTime,
      ability
    )

    if (action.isLegalDelay($strictGameStates.now, deltaTimeWithServer + 100)) {
      socketOutWriter.onNext(
        InGameWSProtocol.GameActionWrapper(
          action :: Nil
        )
      )
    } else if (scala.scalajs.LinkingInfo.developmentMode) {
      dom.console.warn("Entity already casting.")
    }
  }

  var lastTimeStamp = 0L

  private val ticker = (_: Double) => {
    val pressedUserInput = pressedUserInputSignal.now

    val playerMovement = UserInput.movingDirection(pressedUserInput)

    val now       = System.currentTimeMillis + deltaTimeWithServer
    val gameState = $strictGameStates.now
    val deltaTime = now - lastTimeStamp
    lastTimeStamp = now

    gameState.players.get(playerId) match {
      case Some(entity) =>
        val nextPos  = entity.pos + entity.speed * playerMovement * deltaTime / 1000
        val moving   = playerMovement != Complex.zero
        val rotation = $mouseAngleWithPosition.now

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

    val gameStateToDraw = $strictGameStates.now
    gameDrawer.drawGameState(
      gameStateToDraw,
      gameStateToDraw.players.get(playerId).map(_.pos).getOrElse(Complex.zero),
      now
    )
    guiDrawer.update(gameState, now)
  }

  application.ticker.add(ticker)

}
