package gamelogic.abilities.boss.boss103

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.boss.dawnoftime.Boss103
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.gameactions.boss103.PutPurifiedDebuff
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer

/** Puts a [[gamelogic.buffs.boss.boss103.Purified]] debuff on all enemies closer than a certain
  * distance, and deals a certain amount of damage to each of them.
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
  val range: Double     = Boss103.pillarPositionRadius
  val damage: Double    = 30.0

  def name: String = "Sacred Ground"

  def timeToFirstAbility: Long = 0L

  def abilityId: AbilityId = Ability.boss103SacredGroundId
}
