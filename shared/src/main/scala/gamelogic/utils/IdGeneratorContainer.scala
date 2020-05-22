package gamelogic.utils

/**
  * Contains all the generators that are used for the different kind of ids during the game.
  */
final case class IdGeneratorContainer(
    entityIdGenerator: EntityIdGenerator,
    gameActionIdGenerator: GameActionIdGenerator,
    buffIdGenerator: BuffIdGenerator,
    abilityUseIdGenerator: AbilityUseIdGenerator
)

object IdGeneratorContainer {

  def initialIdGeneratorContainer: IdGeneratorContainer = IdGeneratorContainer(
    new EntityIdGenerator(0L),
    new GameActionIdGenerator(0L),
    new BuffIdGenerator(0L),
    new AbilityUseIdGenerator(0L)
  )

}
