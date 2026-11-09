package gamelogic.abilities.pentagon

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.classes.pentagon.SteadyAim
import gamelogic.entities.movingstuff.PentagonBullet
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.NewPentagonBullet
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer
import services.i18n.I18n

/** Like [[CreatePentagonBullet]], but the bullet deals less damage and fractures the enemy it hits
  * (see [[gamelogic.buffs.abilities.classes.Fractured]]). It does not light a facet.
  */
final case class Fracture(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    startingPosition: Complex,
    damage: Double,
    direction: Angle,
    colour: Int
) extends Ability {
  def abilityId: AbilityId = Ability.pentagon.fractureId

  def cooldown: Long = Fracture.cooldown

  def castingTime: Long = Fracture.castingTime

  def cost: Resource.ResourceAmount = Fracture.cost

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] = Vector(
    NewPentagonBullet(
      genActionId(),
      time,
      genEntityId(),
      startingPosition,
      PentagonBullet.defaultSpeed,
      direction,
      PentagonBullet.defaultRange,
      damage * SteadyAim.damageMultiplier(gameState, casterId, time),
      casterId,
      Entity.teams.playerTeam,
      colour,
      fractures = true
    )
  )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long)(using I18n): None.type = None
}

object Fracture {

  inline def cooldown: Long    = 0L
  inline def castingTime: Long = 1000L

  inline def cost: Resource.ResourceAmount = Resource.ResourceAmount(2.0, Resource.Mana)

  inline def damage: Double = 50.0

}
