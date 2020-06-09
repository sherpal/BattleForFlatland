package gamelogic.entities.boss

import gamelogic.abilities.Ability
import gamelogic.entities._
import gamelogic.entities.boss.dawnoftime.Boss102
import gamelogic.physics.shape.Circle

/**
  * A BossEntity is the super class of all Boss instances.
  *
  * A Boss is the main enemy that players have to fight during a boss. There is usually one (but possibly several) boss
  * during a given fight. Once the boss is dead, the game is over and player won.
  *
  * A boss is very similar to a player in what it can do (it has life, it moves and it has abilities). The main
  * difference is that the boss is much more powerful than the players, and they have to team up in order to kill it.
  *
  * A boss often has "adds", which are other entities helping it in some way.
  */
trait BossEntity extends LivingEntity with MovingBody with WithAbilities with WithThreat with WithTarget {

  /**
    * Bosses always have a target (in general one of the players when they attack) and players can know about it.
    *
    * If a boss has no range attack, it will move towards the target in order to hit it. Bosses (and ai entities in
    * general) are faster than players, other players could easily "kite" them to avoid being attacked.
    *
    * The target of the boss (and any ai entity but exceptions) is determined by the player with the biggest threat
    * towards the boss.
    */
  def targetId: Entity.Id

  /**
    * For reference only.
    */
  def name: String

  /**
    * Bosses are always [[gamelogic.physics.shape.Circle]] because that corresponds to the high priests in Flatland,
    * and it corresponds to the lore of the game that players need to fight.
    */
  def shape: Circle

  /** Names of the abilities, to be displayed in the game UI. */
  def abilityNames: Map[Ability.AbilityId, String]

}

object BossEntity {

  private def allBossesFactories: List[BossFactory[_ <: BossEntity]] = List(Boss101, Boss102)

  final def allBossesNames: List[String] = allBossesFactories.map(_.name)

  final def maybeInitialBossByName(name: String, entityId: Entity.Id, time: Long): Option[BossEntity] =
    allBossesFactories.find(_.name == name).map(_.initialBoss(entityId, time))

  final def bossExists(bossName: String): Boolean = allBossesFactories.exists(_.name == bossName)

}
