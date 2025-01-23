package gamelogic.utils

import gamelogic.gamestate.GameAction
import gamelogic.buffs.Buff
import gamelogic.entities.Entity
import gamelogic.abilities.Ability

trait IdsProducer {
  protected inline def genActionId()(using IdGeneratorContainer): GameAction.Id =
    IdsProducer.genActionId()

  protected inline def genBuffId()(using IdGeneratorContainer): Buff.Id =
    IdsProducer.genBuffId()

  protected inline def genEntityId()(using IdGeneratorContainer): Entity.Id =
    IdsProducer.genEntityId()

  protected inline def genAbilityUseId()(using IdGeneratorContainer): Ability.UseId =
    IdsProducer.genAbilityUseId()

}

object IdsProducer {
  inline def genActionId()(using idGen: IdGeneratorContainer): GameAction.Id = idGen.actionId()

  inline def genBuffId()(using idGen: IdGeneratorContainer): Buff.Id = idGen.buffId()

  inline def genEntityId()(using idGen: IdGeneratorContainer): Entity.Id = idGen.entityId()

  inline def genAbilityUseId()(using idGen: IdGeneratorContainer): Ability.UseId =
    idGen.abilityUseId()
}
