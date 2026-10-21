package gamelogic.abilities.boss.boss105

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer

/** Fires a beam from the boss through its target, all the way to the walls of the room. Every
  * player standing on the beam when the cast completes takes damage, the target included.
  *
  * The beam follows the target during the whole cast, so the target can use it to aim: the others
  * have to step aside.
  */
final case class Skewer(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetId: Entity.Id
) extends WithTargetAbility
    with AbilityInfoFromMetadata[Skewer.type] {
  def metadata = Skewer

  def range: Distance = Double.MaxValue

  def cost: Resource.ResourceAmount = Resource.noResourceAmount

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] = {
    val victims = for {
      caster <- gameState.withPositionEntityById(casterId).toVector
      target <- gameState.players.get(targetId).toVector
      source  = caster.currentPosition(time)
      through = target.currentPosition(time)
      player <- gameState.players.valuesIterator
      if player.id == targetId || Skewer.isOnBeam(player.currentPosition(time), source, through)
    } yield player

    victims.map(player =>
      EntityTakesDamage(genActionId(), time, player.id, Skewer.damage, casterId)
    )
  }

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Skewer =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Option[String] =
    Option.unless(gameState.players.contains(targetId))("The target is not there")
}

object Skewer extends AbilityMetadata {

  val cooldown: Long    = 20000L
  val castingTime: Long = 2500L

  val timeToFirstAbility: Long = 10000L

  def name: String = "Skewer"

  def abilityId: AbilityId = Ability.boss105Skewer

  inline def damage: Double = 40.0

  /** Half of the width of the beam. Players are hit when their centre is closer to the axis of the
    * beam than this.
    */
  inline def halfWidth: Double = 30.0

  /** Whether a player standing at `position` is hit by the beam going from `source` through
    * `through`.
    */
  def isOnBeam(position: Complex, source: Complex, through: Complex): Boolean = {
    val direction = (through - source).safeNormalized
    val relative  = position - source
    relative.|*|(direction) >= 0 && math.abs(relative.crossProduct(direction)) <= halfWidth
  }

  /** The players hit by the beam, apart from its target. */
  def playersOnBeam(
      players: Iterable[PlayerClass],
      source: Complex,
      through: Complex,
      time: Long
  ): Vector[PlayerClass] =
    players.filter(player => isOnBeam(player.currentPosition(time), source, through)).toVector
}
