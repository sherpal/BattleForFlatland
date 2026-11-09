package gamelogic.gamestate.gameactions.spiritofnara

import gamelogic.abilities.Ability
import gamelogic.abilities.boss.spiritofnara.Waterbolt
import gamelogic.buffs.Buff
import gamelogic.buffs.ai.DamageThreatAware
import gamelogic.buffs.boss.spiritofnara.Ephemeral
import gamelogic.entities.Entity
import gamelogic.entities.boss.spiritofnara.WaterElemental
import gamelogic.gamestate.GameAction.{EntityCreatorAction, Id}
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import models.syntax.Pointed
import services.i18n.I18n

/** Puts a [[WaterElemental]] in the room. It vanishes after [[Ephemeral.duration]]. */
final case class AddWaterElemental(
    id: GameAction.Id,
    time: Long,
    entityId: Entity.Id,
    position: Complex,
    threatAwareBuffId: Buff.Id,
    ephemeralBuffId: Buff.Id
) extends GameAction
    with EntityCreatorAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    WithEntity(
      WaterElemental(
        entityId,
        time,
        position,
        (-position).arg,
        WaterElemental.maxLife,
        Map.empty,
        entityId,
        Map(
          Ability.spiritOfNara.waterboltId -> Pointed[Waterbolt].unit.copy(
            time = time - Waterbolt.cooldown + Waterbolt.timeToFirstAbility
          )
        )
      ),
      time
    ) ++
      WithBuff(DamageThreatAware(threatAwareBuffId, entityId, entityId, time)) ++
      WithBuff(Ephemeral(ephemeralBuffId, entityId, time))

  def isLegal(gameState: GameState)(using I18n): None.type = None

  def changeId(newId: Id): AddWaterElemental = copy(id = newId)
}
