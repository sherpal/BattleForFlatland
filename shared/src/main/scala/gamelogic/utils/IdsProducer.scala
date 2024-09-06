package gamelogic.utils

import gamelogic.gamestate.GameAction
import gamelogic.buffs.Buff
import gamelogic.entities.Entity
import gamelogic.abilities.Ability

trait IdsProducer {
  protected inline def genActionId()(using idGen: IdGeneratorContainer): GameAction.Id =
    idGen.actionId()

  protected inline def genBuffId()(using idGen: IdGeneratorContainer): Buff.Id =
    idGen.buffId()

  protected inline def genEntityId()(using idGen: IdGeneratorContainer): Entity.Id =
    idGen.entityId()

  protected inline def genAbilityUseId()(using idGen: IdGeneratorContainer): Ability.UseId =
    idGen.abilityUseId()

}
