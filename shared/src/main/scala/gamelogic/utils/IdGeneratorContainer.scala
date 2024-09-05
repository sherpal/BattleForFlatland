package gamelogic.utils

/** Contains all the generators that are used for the different kind of ids during the game.
  */
final case class IdGeneratorContainer(
    entityIdGenerator: EntityIdGenerator,
    gameActionIdGenerator: GameActionIdGenerator,
    buffIdGenerator: BuffIdGenerator,
    abilityUseIdGenerator: AbilityUseIdGenerator
)

object IdGeneratorContainer {

  def initialIdGeneratorContainer: IdGeneratorContainer = IdGeneratorContainer(
    EntityIdGenerator(0L),
    GameActionIdGenerator(0L),
    BuffIdGenerator(0L),
    AbilityUseIdGenerator(0L)
  )

}
