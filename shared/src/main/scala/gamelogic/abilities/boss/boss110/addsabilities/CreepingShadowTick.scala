package gamelogic.abilities.boss.boss110.addsabilities

import gamelogic.docs.AbilityMetadata
import gamelogic.abilities.Ability
import gamelogic.abilities.WithTargetAbility
import gamelogic.docs.AbilityInfoFromMetadata
import gamelogic.entities.Entity
import gamelogic.entities.Resource
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import gamelogic.gamestate.GameState
import gamelogic.entities.boss.boss110.CreepingShadow
import gamelogic.gamestate.gameactions.EntityTakesDamage

/**
  * Cast by the (normaly) unique instance of [[CreepingShadow]] when it collides the boss. It deals damages to
  * every player.
  *
  * The rationale behind this is that the [[CreepingShadow]] moves towards the center of mass of small guies, with
  * a radius proportional to their number. The goal for the players is then to move the small guies away from the boss
  * so that the [[CreepingShadow]] does not cast this.
  */
final case class CreepingShadowTick(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability
    with AbilityInfoFromMetadata[CreepingShadowTick.type] {

  def cost: Resource.ResourceAmount = Resource.noResourceAmount

  def createActions(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    gameState.players.values.toList
      .map(_.id)
      .map(EntityTakesDamage(idGeneratorContainer.gameActionIdGenerator(), time, _, metadata.damageOnTick, casterId))

  def copyWithNewTimeAndId(newTime: Long, newId: Ability.UseId): CreepingShadowTick =
    copy(time = newTime, useId = newId)

  /** [[CreepingShadow]] only casts this if it collides the boss. */
  def canBeCast(gameState: GameState, time: Long): Option[String] =
    (for {
      creepingShadow <- CreepingShadow.extractCreepingShadow(gameState, casterId).toRight("CreepingShadow is not there")
      boss           <- gameState.bosses.get(creepingShadow.sourceId).toRight("Boss is not there")
      _              <- Either.cond(creepingShadow.collides(boss, time), (), "Creeping Shadow does not collide the boss")
    } yield ()).swap.toOption

  def metadata: CreepingShadowTick.type = CreepingShadowTick

}

object CreepingShadowTick extends AbilityMetadata {

  def name: String = "Creeping Shadow Tick"

  def cooldown: Long = 1000L

  def castingTime: Long = 0L

  def timeToFirstAbility: Long = 0L

  def abilityId: Ability.AbilityId = Ability.boss110CreepingShadowTick

  def damageOnTick: Double = 10.0

}
