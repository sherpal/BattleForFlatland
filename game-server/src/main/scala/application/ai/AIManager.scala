package application.ai

import gamelogic.gamestate.GameState
import application.ActionTranslator
import gamelogic.gamestate.GameAction
import gamelogic.entities.Entity
import scala.collection.mutable
import gamelogic.physics.pathfinding.Graph
import gamelogic.gamestate.AddAndRemoveActions
import gamelogic.gamestate.gameactions.*
import gamelogic.entities.classes.Constants
import gamelogic.entities.boss.boss110.BigGuy
import gamelogic.entities.boss.*
import gamelogic.entities.boss.dawnoftime.*
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

class AIManager(gameStateProvider: () => GameState, actionTranslator: ActionTranslator)(using
    ExecutionContext
) {
  actionTranslator.subscribe(handleNewActions)

  private inline def currentGameState: GameState = gameStateProvider()

  private inline def sendActions(gameActions: Vector[GameAction]) =
    actionTranslator.aiNewGameActions(gameActions)

  private inline def now = System.currentTimeMillis()

  def start(): Unit =
    Future {
      // retrieve game state at start of loop
      var gameEnded = false
      while (!gameEnded)
        val gameState = currentGameState
        gameEnded = gameState.ended
        loop(currentGameState)
        Thread.sleep(3)
    }.onComplete {
      case scala.util.Success(_) => println("AIManager loop ended")
      case scala.util.Failure(throwable) =>
        println("[error] AIManager stopped with an error")
        throwable.printStackTrace()
    }

  private val graphManager = GraphManager(
    Vector(Constants.playerRadius, Constants.bossRadius, BigGuy.shape.radius)
  )

  private val aiControllers: mutable.Map[Entity.Id, AIController[?, ?]] = mutable.Map.empty

  def handleNewActions(update: AddAndRemoveActions): Unit = {
    // todo
    val notRemovedActions =
      update.actionsToAdd.filterNot(action => update.idsOfActionsToRemove.contains(action.id))

    val gameState = currentGameState

    notRemovedActions.foreach {
      case action: CreateObstacle =>
        graphManager.addNewObstacle(action, gameState)
      case action: RemoveEntity =>
        graphManager.maybeRemoveObstacle(action)
        aiControllers.remove(action.entityId)
      // todo
      case action: SpawnBoss if action.bossName == Boss101.name =>
        aiControllers.addOne(action.entityId -> boss.Boss101Controller())
      case action: SpawnBoss if action.bossName == Boss102.name =>
        ??? // make boss 102 controller
      case action: SpawnBoss if action.bossName == Boss103.name =>
        ??? // make boss 103 controller
      case action: SpawnBoss if action.bossName == Boss110.name =>
        ??? // make boss 110 controller
      case _ =>
      // ignoring other actions
    }
  }

  def loop(gameState: GameState): Unit = {
    val allEntityIds = gameState.entities.keySet

    aiControllers.filterInPlace((id, _) => allEntityIds.contains(id))

    aiControllers.foreach((id, controller) =>
      controller.computeActions(id, gameState, graphManager.graphs)
    )
  }

}

object AIManager {

  inline def loopRate = 1000 / 60 // 60 fps for ais

}
