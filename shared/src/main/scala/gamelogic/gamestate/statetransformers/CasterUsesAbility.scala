package gamelogic.gamestate.statetransformers

import gamelogic.abilities.Ability
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.GameState

final class CasterUsesAbility(ability: Ability) extends GameStateTransformer {
  def apply(gameState: GameState): GameState =
    gameState.withAbilityEntitiesById(ability.casterId).map(_.useAbility(ability)) match {
      case Some(player: PlayerClass) =>
        gameState.copy(
          time              = ability.time,
          castingEntityInfo = gameState.castingEntityInfo - player.id,
          players           = gameState.players + (player.id -> player)
        )
      case Some(boss: BossEntity) =>
        gameState.copy(
          time              = ability.time,
          castingEntityInfo = gameState.castingEntityInfo - boss.id,
          bosses            = gameState.bosses + (boss.id -> boss)
        )
      case Some(entity) =>
        println(s"Caster uses ability was used on an unknown entity! ($entity)")
        gameState
      case None =>
        gameState
    }
}
