package gamelogic.abilities.hexagon

import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import gamelogic.physics.Complex
import gamelogic.entities.WithPosition.Angle
import gamelogic.abilities.Ability.AbilityId
import gamelogic.entities.Resource
import gamelogic.abilities.WithTargetAbility
import gamelogic.entities.Resource.ResourceAmount
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState
import gamelogic.utils.IdGeneratorContainer
import gamelogic.gamestate.gameactions.classes.hexagon.PutHexagonZone
import utils.misc.RGBAColour
import gamelogic.abilities.Ability.UseId

final case class CreateHexagonZone(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    position: Complex,
    heal: Double,
    rotation: Angle,
    colour: RGBAColour
) extends Ability {
  def abilityId: AbilityId = Ability.hexagonHexagonZoneId

  def cooldown: Long = CreateHexagonZone.cooldown

  def castingTime: Long = CreateHexagonZone.castingTime

  def cost: ResourceAmount = CreateHexagonZone.cost

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(
      PutHexagonZone(
        genActionId(),
        time,
        genEntityId(),
        position,
        rotation,
        heal,
        casterId,
        colour,
        genBuffId()
      )
    )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Option[String] =
    (for
      player <- gameState.players
        .get(casterId)
        .toRight(s"Player $casterId does not exist (probably dead)")
      _ <- Option
        .unless((player.pos - position).modulus < CreateHexagonZone.range)("Not in range")
        .toLeft(())
    yield ()).swap.toOption
}

object CreateHexagonZone {

  inline def cooldown    = 20000L
  inline def castingTime = 1500L

  val cost = Resource.ResourceAmount(20.0, Resource.Mana)

  inline def range: Double = WithTargetAbility.healRange

}
