package gamelogic.entities.boss
import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.physics.Complex
import gamelogic.physics.shape.Circle

/**
  * Very first boss to be coded. Probably not the most exiting one but the goal was to have a first proof of concept
  * and retrieve as much feedback as possible.
  *
  * The abilities of the boss are the following:
  * - "big" hit that directly attack the target (with a casting time so that the player can react). This attack will
  *   probably kill any player other than a tank (under cd)
  * - dot placed on someone different from the target (no casting time)
  * - spawn adds which will move towards and attack the player with the biggest healing threat (the heal if he is the
  *   only one). These adds will have melee attacks and move not too fast, leaving time to dps to kill them before
  *   they reach the heal.
  *
  * This boss is intended for 4 players (1 tank, 2 dps and 1 healer)
  */
final case class Boss101(
    id: Entity.Id,
    time: Long,
    targetId: Entity.Id,
    pos: Complex,
    rotation: Angle,
    direction: Angle,
    speed: Double,
    moving: Boolean,
    life: Double,
    maxLife: Double,
    relevantUsedAbilities: Map[AbilityId, Ability]
) extends BossEntity {

  def name: String = "Boss 101"

  def shape: Circle = Boss101.shape

  def abilities: Set[AbilityId] = Set.empty

  def useAbility(ability: Ability): Boss101 = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability)
  )

  def maxResourceAmount: Double      = 0.0
  def resourceAmount: ResourceAmount = ResourceAmount(0, NoResource)

  def move(time: Long, position: Complex, direction: Angle, rotation: Angle, speed: Double, moving: Boolean): Boss101 =
    copy(time = time, pos = position, direction = direction, rotation = rotation, speed = speed, moving = moving)

  protected def patchLifeTotal(newLife: Double): Boss101 =
    copy(life = newLife)

}

object Boss101 {
  final val shape: Circle = new Circle(30.0)
}
