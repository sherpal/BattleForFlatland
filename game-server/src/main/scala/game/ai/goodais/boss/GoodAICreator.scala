package game.ai.goodais.boss

import gamelogic.entities.Entity
import models.bff.outofgame.gameconfig.PlayerName
import gamelogic.docs.BossMetadata
import akka.actor.typed.ActorRef
import game.ai.goodais.GoodAIManager
import gamelogic.entities.boss.Boss101
import models.bff.outofgame.PlayerClasses
import game.ai.goodais.boss.boss101.{HealForBoss101, SquareForBoss101}
import gamelogic.gamestate.GameState
import akka.actor.typed.Behavior
import game.ActionTranslator
import game.ai.goodais.GoodAIController
import game.ai.goodais.boss.boss101.PentagonForBoss101

trait GoodAICreator[BossType <: BossMetadata] {

  import GoodAICreator.ActionTranslatorRef

  type GameStateWrapper

  def apply(
      entityId: Entity.Id,
      actionCollector: ActionTranslatorRef
  ): Behavior[GameStateWrapper]

  def gameStateWrapper(gameState: GameState): GameStateWrapper

}

object GoodAICreator {

  type ActionTranslatorRef = ActorRef[ActionTranslator.GameActionsWrapper]

  val squareForBoss101Creator = new GoodAICreator[Boss101.type] {
    type GameStateWrapper = GoodAIController.GameStateWrapper
    def apply(
        entityId: Entity.Id,
        actionCollector: ActionTranslatorRef
    ): Behavior[GameStateWrapper] = SquareForBoss101(entityId, actionCollector).narrow[GameStateWrapper]
    def gameStateWrapper(gameState: GameState): GameStateWrapper =
      GoodAIController.GameStateWrapper(gameState)
  }

  private def hexagonForBoss101Creator(index: Int) = new GoodAICreator[Boss101.type] {
    type GameStateWrapper = GoodAIController.GameStateWrapper
    def apply(entityId: Entity.Id, actionCollector: ActionTranslatorRef): Behavior[GameStateWrapper] =
      new HealForBoss101(index).apply(entityId, actionCollector).narrow[GameStateWrapper]
    def gameStateWrapper(gameState: GameState): GameStateWrapper =
      GoodAIController.GameStateWrapper(gameState)
  }

  private def pentagonForBoss101Creator(index: Int) = new GoodAICreator[Boss101.type] {
    type GameStateWrapper = GoodAIController.GameStateWrapper
    def apply(entityId: Entity.Id, actionCollector: ActionTranslatorRef): Behavior[GameStateWrapper] =
      new PentagonForBoss101(index)(entityId, actionCollector).narrow[GameStateWrapper]
    def gameStateWrapper(gameState: GameState): GameStateWrapper =
      GoodAIController.GameStateWrapper(gameState)
  }

  def maybeCreatorByBossMetadata(
      metadata: BossMetadata,
      playerName: PlayerName.AIPlayerName
  ): Option[GoodAICreator[_ <: BossMetadata]] =
    (metadata, playerName) match {
      case (Boss101, SquareForBoss101.name) =>
        Some(squareForBoss101Creator)
      case (Boss101, PlayerName.AIPlayerName(PlayerClasses.Hexagon, index)) =>
        Some(hexagonForBoss101Creator(index))
      case (Boss101, PlayerName.AIPlayerName(PlayerClasses.Pentagon, index)) =>
        Some(pentagonForBoss101Creator(index))
      case _ => Option.empty
    }

}
