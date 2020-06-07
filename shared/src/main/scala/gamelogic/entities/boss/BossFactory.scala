package gamelogic.entities.boss

import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer

trait BossFactory {

  /** Describes how to create the boss at the beginning of the game. */
  def initialBoss(entityId: Entity.Id, time: Long): BossEntity

  /** Returns where the boss will initially start. */
  final def bossStartingPosition: Complex = initialBoss(0L, 0L).pos

  /**
    * Describes all actions to take immediately after creating the boss.
    * These actions will most probably contain the heal aware and threat aware buffs.
    */
  def initialBossActions(entityId: Entity.Id, time: Long, idGeneratorContainer: IdGeneratorContainer): List[GameAction]

  /**
    * Describes all actions to take at the origin of time.
    * This can be used, for example, to add obstacles to the game at the very beginning.
    */
  def stagingBossActions(time: Long, idGeneratorContainer: IdGeneratorContainer): List[GameAction]

  /** Where the players should be created. */
  def playersStartingPosition: Complex

  /** Name of the boss. */
  def name: String

}

object BossFactory {

  val factoriesByBossName: Map[String, BossFactory] = List(
    Boss101
  ).map(factory => factory.name -> factory).toMap

}
