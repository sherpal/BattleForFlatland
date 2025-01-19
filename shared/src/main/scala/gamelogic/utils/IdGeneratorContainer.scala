package gamelogic.utils

import gamelogic.gamestate.GameAction
import gamelogic.entities.Entity
import gamelogic.buffs.Buff
import gamelogic.abilities.Ability

/** Contains all the generators that are used for the different kind of ids during the game.
  */
final class IdGeneratorContainer(
    entityIdGenerator: EntityIdGenerator,
    gameActionIdGenerator: GameActionIdGenerator,
    buffIdGenerator: BuffIdGenerator,
    abilityUseIdGenerator: AbilityUseIdGenerator
) {
  inline def actionId(): GameAction.Id = gameActionIdGenerator()

  inline def entityId(): Entity.Id = entityIdGenerator()

  inline def buffId(): Buff.Id = buffIdGenerator()

  inline def abilityUseId(): Ability.UseId = abilityUseIdGenerator()
}

object IdGeneratorContainer {

  def initialIdGeneratorContainer: IdGeneratorContainer = IdGeneratorContainer(
    EntityIdGenerator(Entity.Id.zero),
    GameActionIdGenerator(GameAction.Id.zero),
    BuffIdGenerator(Buff.Id.zero),
    AbilityUseIdGenerator(Ability.UseId.zero)
  )

}
