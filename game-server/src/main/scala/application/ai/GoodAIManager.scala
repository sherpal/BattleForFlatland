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
import application.TimeManager
import application.ai.goodais.GoodAIController
import gamelogic.docs.BossMetadata
import models.bff.outofgame.PlayerClasses.*
import models.bff.outofgame.gameconfig.PlayerName
import application.ai.goodais.bosses.boss101.*
import application.ai.goodais.bosses.boss102.*
import application.ai.goodais.bosses.boss104.{
  Boss104Container,
  PentagonForBoss104,
  SquareForBoss104,
  TriangleForBoss104
}
import application.ai.goodais.classes.{
  HexagonAIController,
  PentagonAIController,
  SquareAIController,
  TriangleAIController
}
import models.bff.outofgame.PlayerClasses

class GoodAIManager(
    bossMetadata: BossMetadata,
    gameStateProvider: () => GameState,
    actionTranslator: ActionTranslator
)(using
    ExecutionContext
) extends TimeManager {
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
        sleep(5)
    }.onComplete {
      case scala.util.Success(_) => println("GoodAIManager loop ended")
      case scala.util.Failure(throwable) =>
        println("[error] GoodAIManager stopped with an error")
        throwable.printStackTrace()
    }

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
      case action: AddPlayerByClass =>
        action.playerClass.parsePlayerName(action.playerName).foreach {
          case PlayerName.AIPlayerName(cls, index) =>
            GoodAIManager.bossAIContainers.get(bossMetadata) match {
              case Some(container) =>
                aiControllers
                  .addOne(action.entityId -> container.controller(cls)(index, action.entityId))
              case None =>
                println(s"I don't handle boss ${bossMetadata.name}")
            }
        }
      // todo
      case _ =>
      // ignoring other actions
    }
  }

  private val graphManager = GraphManager(
    Vector(math.round(Constants.playerRadius).toInt)
  )

  private val aiControllers: mutable.Map[Entity.Id, GoodAIController[?]] = mutable.Map.empty

  def loop(gameState: GameState): Unit = {
    val allEntityIds = gameState.entities.keySet

    aiControllers.filterInPlace((id, _) => allEntityIds.contains(id))

    aiControllers.values.foreach { controller =>
      val controllerActions = controller.computeActions(gameState, graphManager.graphs)
      actionTranslator.aiNewGameActions(controllerActions)
    }
  }

}

object GoodAIManager {

  inline def loopRate: Int = 1000 / 60 // 60 fps for ais

  trait BossAIContainer {
    def triangle(index: Int, id: Entity.Id): TriangleAIController
    def square(index: Int, id: Entity.Id): SquareAIController
    def pentagon(index: Int, id: Entity.Id): PentagonAIController
    def hexagon(index: Int, id: Entity.Id): HexagonAIController

    final def controller(cls: PlayerClasses): (Int, Entity.Id) => GoodAIController[?] = cls match {
      case PlayerClasses.Square   => square
      case PlayerClasses.Hexagon  => hexagon
      case PlayerClasses.Triangle => triangle
      case PlayerClasses.Pentagon => pentagon
    }
  }

  private val bossAIContainers: Map[BossMetadata, BossAIContainer] = Map(
    Boss101 -> Boss101Container(),
    Boss102 -> Boss102Container(),
    Boss104 -> Boss104Container()
  )

}
