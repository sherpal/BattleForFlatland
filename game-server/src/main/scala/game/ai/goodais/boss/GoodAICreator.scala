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
import game.ai.goodais.boss.boss101.TriangleForBoss101

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

  private class DefaultAICreator[BossType <: BossMetadata](
      behavior: (Entity.Id, ActionTranslatorRef) => Behavior[GoodAIController.Command]
  ) extends GoodAICreator[BossType] {
    type GameStateWrapper = GoodAIController.GameStateWrapper
    def apply(
        entityId: Entity.Id,
        actionCollector: ActionTranslatorRef
    ): Behavior[GameStateWrapper] = behavior(entityId, actionCollector).narrow[GameStateWrapper]
    def gameStateWrapper(gameState: GameState): GameStateWrapper =
      GoodAIController.GameStateWrapper(gameState)
  }

  def maybeCreatorByBossMetadata(
      metadata: BossMetadata,
      playerName: PlayerName.AIPlayerName
  ): Option[GoodAICreator[_ <: BossMetadata]] =
    (metadata, playerName) match {
      case (Boss101, SquareForBoss101.name) =>
        Some(new DefaultAICreator[Boss101.type](SquareForBoss101(_, _)))
      case (Boss101, PlayerName.AIPlayerName(PlayerClasses.Hexagon, index)) =>
        Some(new DefaultAICreator[Boss101.type](new HealForBoss101(index)(_, _)))
      case (Boss101, PlayerName.AIPlayerName(PlayerClasses.Pentagon, index)) =>
        Some(new DefaultAICreator[Boss101.type](new PentagonForBoss101(index)(_, _)))
      case (Boss101, PlayerName.AIPlayerName(PlayerClasses.Triangle, index)) =>
        Some(new DefaultAICreator[Boss101.type](new TriangleForBoss101(index)(_, _)))
      case _ => Option.empty
    }

}
