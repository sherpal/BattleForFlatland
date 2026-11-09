package gamelogic.abilities.boss.spiritofnara

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.spiritofnara.AddWaterElemental
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer
import services.i18n.I18n

/** Each time it falls under one of the [[SummonWaterElementals.lifeThresholds]] of its life, the
  * Spirit of Nara summons [[SummonWaterElementals.count]]
  * [[gamelogic.entities.boss.spiritofnara.WaterElemental]]s along the walls.
  *
  * @param wave
  *   which threshold this summon is for (1 for the first one). The boss remembers its last use of
  *   the ability, hence the last wave it summoned. Before the first one, it is 0.
  */
final case class SummonWaterElementals(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    wave: Int
) extends Ability
    with AbilityInfoFromMetadata[SummonWaterElementals.type] {
  def metadata = SummonWaterElementals

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0, Resource.Mana)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    SummonWaterElementals.positions.map(position =>
      AddWaterElemental(genActionId(), time, genEntityId(), position, genBuffId(), genBuffId())
    )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): SummonWaterElementals =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long)(using I18n): None.type = None
}

object SummonWaterElementals extends AbilityMetadata {
  val name: String = "Summon Water Elementals"

  /** Waves are at least that far apart, even if the boss falls under both thresholds at once. */
  val cooldown: Long           = 20000L
  val castingTime: Long        = 0L
  val timeToFirstAbility: Long = 0L

  /** Under each of these fractions of its life, the boss summons a wave. */
  val lifeThresholds: Vector[Double] = Vector(0.4, 0.2)
  inline def count: Int              = 3

  /** Along the walls, evenly spread. */
  val positions: Vector[Complex] = (0 until count).toVector.map(index =>
    Complex.polar(330, Angle.Tau * Angle.fromDouble(index.toDouble / count) + Angle.Pi / 2)
  )

  def abilityId: AbilityId = Ability.spiritOfNara.summonElementalsId

  /** The wave to summon now, if the boss is under the threshold of the wave following the last one.
    *
    * @param lastWave
    *   the last wave summoned (0 if none)
    */
  def dueWave(lastWave: Int, lifeRatio: Double): Option[Int] =
    lifeThresholds.lift(lastWave).filter(lifeRatio <= _).map(_ => lastWave + 1)
}
