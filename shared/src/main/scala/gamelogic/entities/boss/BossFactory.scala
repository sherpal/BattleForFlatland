package gamelogic.entities.boss

import gamelogic.buffs.Buff
import gamelogic.entities.Entity
import gamelogic.entities.boss.dawnoftime.{Boss102, Boss103}
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.PutSimpleBuff
import gamelogic.physics.Complex
import gamelogic.utils.{IdGeneratorContainer, IdsProducer}
import gamelogic.gamestate.GameState
import gamelogic.entities.boss.dawnoftime.Boss110

trait BossFactory[Boss <: BossEntity] extends IdsProducer {

  /** Describes how to create the boss at the beginning of the game. */
  def initialBoss(entityId: Entity.Id, time: Long): Boss

  /** Returns where the boss will initially start. */
  final def bossStartingPosition: Complex = initialBoss(Entity.Id.zero, 0L).pos

  /** Describes all actions to take immediately after creating the boss. These actions will most
    * probably contain the heal aware and threat aware buffs.
    *
    * @param entityId
    *   id of the boss that was created.
    */
  def initialBossActions(entityId: Entity.Id, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction]

  /** Describes all actions to take at the origin of time. This can be used, for example, to add
    * obstacles to the game at the very beginning.
    */
  def stagingBossActions(time: Long)(using IdGeneratorContainer): Vector[GameAction]

  /** Describes all actions to take when the boss dies at the end of the game. Typically, this can
    * be used to clean up some remaining things, and probably prevent players from dying because of
    * a remaining debuff or remaining adds.
    *
    * @param gameState
    *   [[GameState]] when the game ends
    * @param time
    *   time when the game ends.
    */
  def whenBossDiesActions(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction]

  /** Where the players should be created. */
  def playersStartingPosition: Complex

  /** Name of the boss. */
  def name: String

  /** Returns the two actions so that the boss have the healing and damage aware buff for actions.
    */
  final def healAndDamageAwareActions(
      entityId: Entity.Id,
      time: Long
  )(using IdGeneratorContainer): Vector[GameAction] = Vector(
    PutSimpleBuff(
      GameAction.Id.zero,
      time,
      genBuffId(),
      entityId,
      entityId,
      time,
      Buff.healingThreatAware
    ),
    PutSimpleBuff(
      GameAction.Id.zero,
      time,
      genBuffId(),
      entityId,
      entityId,
      time,
      Buff.damageThreatAware
    )
  )

}

object BossFactory {

  // todo[scala3] change this by imposing that the name of the boss must map to the correct boss factory with match types
  val factoriesByBossName: Map[String, BossFactory[? <: BossEntity]] = List(
    Boss101Dev,
    Boss101,
    Boss102,
    Boss103,
    Boss110
  ).map(factory => factory.name -> factory).toMap

}
