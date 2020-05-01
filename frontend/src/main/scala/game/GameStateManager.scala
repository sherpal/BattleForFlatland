package game

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.Owner
import com.raquo.airstream.signal.{Signal, SignalViewer}
import gamelogic.entities.{DummyLivingEntity, Entity}
import gamelogic.gamestate.gameactions.DummyEntityMoves
import gamelogic.gamestate.{ActionCollector, AddAndRemoveActions, GameAction, GameState}
import gamelogic.physics.Complex
import models.bff.ingame.{InGameWSProtocol, UserInput}
import typings.pixiJs.mod.Application
import typings.pixiJs.{AnonAntialias => ApplicationOptions}
import utils.pixi.monkeypatching.PIXIPatching._

import scala.util.Random

final class GameStateManager(
    initialGameState: GameState,
    $actionsFromServer: EventStream[AddAndRemoveActions],
    socketOutWriter: Observer[InGameWSProtocol.Outgoing],
    keyboard: Keyboard,
    playerId: Entity.Id
)(implicit owner: Owner) {

  val application: Application                 = new Application(ApplicationOptions(backgroundColor = 0x1099bb))
  private val actionCollector: ActionCollector = new ActionCollector(initialGameState)
  private val gameDrawer                       = new GameDrawer(application)

  private val gameStateBus: EventBus[GameState] = new EventBus[GameState]

  val $gameStates: Signal[GameState] = gameStateBus.events.startWith(initialGameState)
  private val $strictGameStates      = $gameStates.observe

  private var unconfirmedActions: List[GameAction] = Nil

  private def nextGameState(): Unit =
    gameStateBus.writer.onNext(actionCollector.currentGameState.applyActions(unconfirmedActions))

  $actionsFromServer.foreach {
    case AddAndRemoveActions(actionsToAdd, oldestTimeToRemove, idsOfActionsToRemove) =>
      actionsToAdd.foreach(actionCollector.addAction(_, needUpdate = false))
      actionCollector.removeActions(oldestTimeToRemove, idsOfActionsToRemove)

      unconfirmedActions = unconfirmedActions.dropWhile(_.time < actionCollector.currentGameState.time)

      nextGameState()
  }

  val pressedUserInputSignal: SignalViewer[Set[UserInput]] = keyboard.$pressedUserInput.observe

  var lastTimeStamp = 0L

  private val ticker = (_: Double) => {
    val pressedUserInput = pressedUserInputSignal.now

    val playerMovement = UserInput.movingDirection(pressedUserInput)

    val now       = System.currentTimeMillis
    val gameState = $strictGameStates.now
    val deltaTime = now - lastTimeStamp
    lastTimeStamp = now

    gameState.players.get(playerId) match {
      case Some(entity) =>
        val nextPos = entity.pos + entity.speed * playerMovement * deltaTime / 1000
        val moving  = playerMovement != Complex.zero

        if (moving || entity.moving != moving) {
          val newAction = DummyEntityMoves(
            0L,
            now,
            playerId,
            nextPos,
            moving,
            entity.direction,
            entity.colour
          )
          unconfirmedActions = unconfirmedActions :+ newAction

          socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(List(newAction)))

          nextGameState()
        }
      case None =>
      // do nothing, player is dead
    }

    gameDrawer.drawGameState($strictGameStates.now, 0)
  }

  application.ticker.add(ticker)

}
