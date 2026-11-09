package gamelogic.abilities.pentagon

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.classes.pentagon.SteadyAim
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.classes.pentagon.PutPentagonZone
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer
import services.i18n.I18n
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
  def abilityId: AbilityId = Ability.pentagon.pentagonZoneId

  def cooldown: Long = CreatePentagonZone.cooldown

  def castingTime: Long = CreatePentagonZone.castingTime

  def cost: Resource.ResourceAmount = CreatePentagonZone.cost

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(
      PutPentagonZone(
        genActionId(),
        time,
        genEntityId(),
        position,
        rotation,
        damage * SteadyAim.damageMultiplier(gameState, casterId, time),
        casterId,
        colour,
        genBuffId()
      )
    )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long)(using i18n: I18n): Option[String] =
    (for {
      player <- gameState.players
        .get(casterId)
        .toRight(i18n.youAreDead)
      _ <- Option
        .unless((player.pos - position).modulus < CreatePentagonZone.range)(i18n.notInRange)
        .toLeft(())
      _ <- Option
        .unless(gameState.isPositionInSight(player, position, time))(
          i18n.positionNotInSight
        )
        .toLeft(())
    } yield ()).swap.toOption
}

object CreatePentagonZone {

  /** Slightly longer than the zone lasts: it has to be put where the enemies will stay. */
  inline def cooldown: Long                = 12000L
  inline def cost: Resource.ResourceAmount = Resource.ResourceAmount(10.0, Resource.Mana)
  inline def castingTime: Long             = 1500L
  inline def range: Double                 = WithTargetAbility.healRange

}
