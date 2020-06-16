package gamelogic.abilities.pentagon

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.classes.pentagon.PentagonZone
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.classes.pentagon.PutPentagonZone
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer
import utils.misc.RGBAColour

case class CreatePentagonZone(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    position: Complex,
    damage: Double,
    rotation: Angle,
    colour: RGBAColour
) extends Ability {
  def abilityId: AbilityId = Ability.createPentagonZoneId

  def cooldown: Long = CreatePentagonZone.cooldown

  def castingTime: Long = CreatePentagonZone.castingTime

  def cost: Resource.ResourceAmount = CreatePentagonZone.cost

  def createActions(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    List(
      PutPentagonZone(
        idGeneratorContainer.gameActionIdGenerator(),
        time,
        idGeneratorContainer.entityIdGenerator(),
        position,
        rotation,
        damage,
        casterId,
        colour,
        idGeneratorContainer.buffIdGenerator()
      )
    )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability = copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Boolean =
    gameState.players.get(casterId).fold(false)(player => (player.pos - position).modulus < CreatePentagonZone.range)
}

object CreatePentagonZone {

  @inline final def cooldown: Long                = PentagonZone.duration
  @inline final def cost: Resource.ResourceAmount = Resource.ResourceAmount(10.0, Resource.Mana)
  @inline final def castingTime: Long             = 1500L
  @inline final def range: Double                 = WithTargetAbility.healRange

}
