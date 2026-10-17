package gamelogic.abilities.boss.sirofvice

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.gameactions.sirofvice.PutPurifiedDebuff
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer

/** Puts a [[gamelogic.buffs.boss.sirofvice.Purified]] debuff on all enemies closer than `radius`
  * from `position`, and deals a certain amount of damage to each of them.
  *
  * This is the "aura" of the boss: its AI uses it each time it is available, centred on itself.
  *
  * The amount of damage that each player takes is given by [[SacredGround#damage]] divided by
  *   - 1 if there is only one player hit
  *   - 4 if there are at least two players hit
  *   - 5 if more than two players are hit.
  */
final case class SacredGround(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    position: Complex,
    radius: Double
) extends Ability
    with AbilityInfoFromMetadata[SacredGround.type] {
  def metadata = SacredGround

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0, Resource.NoResource)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] = {
    val playersWhoGetHit = gameState.players.valuesIterator
      .filter(_.currentPosition(time).distanceTo(position) < radius)
      .toVector

    val nbrPlayerGotHit = playersWhoGetHit.length
    val damageToEachPlayer =
      (SacredGround.damage / (math.pow(nbrPlayerGotHit max 1, 2) min 5.0)).toInt.toDouble

    playersWhoGetHit
      .flatMap(player =>
        Vector(
          PutPurifiedDebuff(
            id = genActionId(),
            time = time,
            buffId = genBuffId(),
            bearerId = player.id,
            sourceId = casterId
          ),
          EntityTakesDamage(
            genActionId(),
            time,
            player.id,
            damageToEachPlayer,
            casterId
          )
        )
      )
  }

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): None.type = None
}

object SacredGround extends AbilityMetadata {

  val cooldown: Long    = 2000L
  val castingTime: Long = 0L

  /** Radius of the aura. Melee players are in it, the others must stay out of it. */
  val radius: Double = 180.0
  val damage: Double = 30.0

  def name: String = "Sacred Ground"

  def timeToFirstAbility: Long = 0L

  def abilityId: AbilityId = Ability.sirOfViceSacredGround
}
