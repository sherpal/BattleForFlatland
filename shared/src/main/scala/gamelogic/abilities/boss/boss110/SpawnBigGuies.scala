package gamelogic.abilities.boss.boss110

import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import gamelogic.entities.Resource
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import gamelogic.gamestate.GameState
import gamelogic.docs.AbilityMetadata
import gamelogic.physics.Complex
import gamelogic.entities.boss.dawnoftime.Boss110
import gamelogic.gamestate.gameactions.boss110.AddBigGuies

final case class SpawnBigGuies(useId: Ability.UseId, time: Long, casterId: Entity.Id) extends Ability {

  def abilityId: Ability.AbilityId = Ability.boss110SpawnBigGuies

  def cooldown: Long = SpawnBigGuies.cooldown

  def castingTime: Long = SpawnBigGuies.castingTime

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0.0, Resource.NoResource)

  def createActions(gameState: GameState)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] = List(
    AddBigGuies(
      idGeneratorContainer.gameActionIdGenerator(),
      time,
      SpawnBigGuies.bigGuiesPositions.map(idGeneratorContainer.entityIdGenerator() -> _)
    )
  )

  def copyWithNewTimeAndId(newTime: Long, newId: Ability.UseId): SpawnBigGuies =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Option[String] = Option.empty

}

object SpawnBigGuies extends AbilityMetadata {

  def name: String = "Spawn Big Guies"

  def cooldown: Long = 30000L

  def castingTime: Long = 1500L

  def timeToFirstAbility: Long = 10000L

  import Complex.DoubleWithI

  val bigGuiesPositions: List[Complex] =
    List(
      -1,
      0,
      1
    ).map(_ * Boss110.halfHeight.toDouble.i * 3 / 5)
      .map(_ + Boss110.halfWidth * 3 / 5)

}
