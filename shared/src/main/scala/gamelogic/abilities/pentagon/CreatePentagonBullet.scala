package gamelogic.abilities.pentagon

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.movingstuff.PentagonBullet
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.NewPentagonBullet
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer

/**
  * Spawn a [[gamelogic.entities.movingstuff.PentagonBullet]] at the player's position, in the given direction.
  *
  * Will deal damage equal to the specified amount.
  */
case class CreatePentagonBullet(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    startingPosition: Complex,
    damage: Double,
    direction: Angle,
    colour: Int
) extends Ability {
  def abilityId: AbilityId = Ability.pentagonPentagonBullet

  def cooldown: Long = CreatePentagonBullet.cooldown

  def castingTime: Long = CreatePentagonBullet.castingTime

  def cost: Resource.ResourceAmount = CreatePentagonBullet.cost

  def createActions(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] = List(
    NewPentagonBullet(
      idGeneratorContainer.gameActionIdGenerator(),
      time,
      idGeneratorContainer.entityIdGenerator(),
      startingPosition,
      PentagonBullet.defaultSpeed,
      direction,
      PentagonBullet.defaultRange,
      damage,
      casterId,
      Entity.teams.playerTeam,
      colour
    )
  )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability = copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): None.type = None
}

object CreatePentagonBullet {

  final val cooldown    = 0L
  final val castingTime = 500L

  final val cost = Resource.ResourceAmount(2.0, Resource.Mana)

  final def damage: Double = 40.0 // approx match triangle direct hit

}
