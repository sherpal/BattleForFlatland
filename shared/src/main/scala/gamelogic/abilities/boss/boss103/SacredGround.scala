package gamelogic.abilities.boss.boss103

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.buffs.boss.boss103.Purified
import gamelogic.entities.boss.dawnoftime.Boss103
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.gameactions.boss103.PutPurifiedDebuff
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/**
  * Puts a [[Purified]] debuff on all enemies closer than a certain distance, and deals a certain amount of damage to
  * each of them.
  *
  * The amount of damage that each player takes is given by [[SacredGround#damage]] divided by
  * - 1 if there is only one player hit
  * - 4 if there are at least two players hit
  * - 5 if more than two players are hit.
  */
final case class SacredGround(useId: Ability.UseId, time: Long, casterId: Entity.Id) extends Ability {
  def abilityId: AbilityId = Ability.boss103SacredGroundId

  def cooldown: Long = SacredGround.cooldown

  def castingTime: Long = SacredGround.castingTime

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0, Resource.NoResource)

  def createActions(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    gameState.movingBodyEntityById(castingTime).fold(List.empty[GameAction]) { boss =>
      val playersWhoGetHit = gameState.players.valuesIterator
        .filter(_.currentPosition(time).distanceTo(boss.currentPosition(time)) < SacredGround.range)
        .toList

      val nbrPlayerGotHit    = playersWhoGetHit.length
      val damageToEachPlayer = SacredGround.damage / (math.pow(nbrPlayerGotHit max 1, 2) min 5.0)

      playersWhoGetHit
        .flatMap(
          player =>
            List(
              PutPurifiedDebuff(
                id       = idGeneratorContainer.gameActionIdGenerator(),
                time     = time,
                buffId   = idGeneratorContainer.buffIdGenerator(),
                bearerId = player.id,
                sourceId = casterId
              ),
              EntityTakesDamage(
                idGeneratorContainer.gameActionIdGenerator(),
                time,
                player.id,
                damageToEachPlayer,
                casterId
              )
            )
        )
    }

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability = copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Boolean = true
}

object SacredGround {

  val cooldown: Long    = 2000L
  val castingTime: Long = 0L
  val range: Double     = Boss103.pillarPositionRadius
  val damage: Double    = 30.0

}
