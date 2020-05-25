package gamelogic.entities.boss

import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction
import gamelogic.utils.IdGeneratorContainer

trait BossFactory {

  def initialBoss(entityId: Entity.Id, time: Long): BossEntity

  def initialBossActions(entityId: Entity.Id, time: Long, idGeneratorContainer: IdGeneratorContainer): List[GameAction]

  def name: String

}

object BossFactory {

  val factoriesByBossName: Map[String, BossFactory] = List(
    Boss101
  ).map(factory => factory.name -> factory).toMap

}
