package gamelogic.entities.boss

import gamelogic.entities.Entity

trait BossFactory {

  def initialBoss(entityId: Entity.Id, time: Long): BossEntity

  def name: String

}
