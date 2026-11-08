package gamelogic.gamestate.gameactions

import gamelogic.abilities.Ability
import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, DoT}
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}
import gamelogic.gamestate.{GameAction, GameState}
import services.i18n.I18n

final case class PutConstantDot(
    id: GameAction.Id,
    time: Long,
    targetId: Entity.Id,
    sourceId: Entity.Id,
    damageOnTick: Double,
    duration: Long,
    tickRate: Long,
    buffId: Buff.Id,
    resourceIdentifier: ResourceIdentifier,
    abilityId: Ability.AbilityId
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState
      .livingEntityById(targetId)
      .fold(GameStateTransformer.identityTransformer) { _ =>
        new WithBuff(
          DoT.constantDot(
            time,
            targetId,
            buffId,
            duration,
            tickRate,
            damageOnTick,
            sourceId,
            time,
            resourceIdentifier,
            abilityId
          )
        )
      }

  def isLegal(gameState: GameState)(using i18n: I18n): Option[String] =
    gameState.livingEntityById(targetId) match {
      case None => Some(i18n.entityIsNotLiving(targetId))
      case _ => None
    }

  def changeId(newId: Id): GameAction = copy(id = newId)
}
