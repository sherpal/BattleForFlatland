package gamelogic.abilities.boss.boss110

import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import gamelogic.entities.Resource
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import gamelogic.gamestate.GameState
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.physics.Complex
import gamelogic.entities.boss.dawnoftime.Boss110
import gamelogic.gamestate.gameactions.boss110.AddSmallGuy

final case class SpawnSmallGuies(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability
    with AbilityInfoFromMetadata[SpawnSmallGuies.type] {

  def metadata: SpawnSmallGuies.type = SpawnSmallGuies

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0.0, Resource.NoResource)

  def createActions(gameState: GameState)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] = SpawnSmallGuies.startingPositions.map { position =>
    AddSmallGuy(idGeneratorContainer.gameActionIdGenerator(), time, idGeneratorContainer.entityIdGenerator(), position)
  }

  def copyWithNewTimeAndId(newTime: Long, newId: Ability.UseId): SpawnSmallGuies =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Option[String] = Option.empty

}

object SpawnSmallGuies extends AbilityMetadata {

  def name: String = "Spawn Small Guies"

  def cooldown: Long = 5100L

  def castingTime: Long = 0L

  def timeToFirstAbility: Long = 1000L

  import Complex.i

  val startingPositions: List[Complex] = List(-i, i).map(_ * 50).map(_ - Boss110.halfWidth * 4 / 5)

  def abilityId: Ability.AbilityId = Ability.boss110SpawnSmallGuies

}
