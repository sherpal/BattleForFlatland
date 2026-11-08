package application.ai.goodais.bosses.spiritofnara

import application.ai.goodais.classes.PentagonAIController
import gamelogic.buffs.boss.spiritofnara.Chained
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.firstraid.SpiritOfNara
import gamelogic.entities.boss.spiritofnara.WaterElemental
import gamelogic.entities.classes.Pentagon
import gamelogic.gamestate.{GameAction, GameState}

/** Ranged damage dealers. They free the chained players (urgently when the Arcane Explosion is
  * coming), and kill the Water Elementals before going back to the boss.
  *
  * The composition has two Pentagons: `index` gives each of them its own spot, and makes the second
  * one react a bit later, so that they don't both dispel the same player.
  */
final case class PentagonForSpiritOfNara(index: Int, entityId: Entity.Id)
    extends PentagonAIController(index)
    with SpiritOfNaraBot[Pentagon] {

  def homeAngle: Angle =
    Angle.Pi * Angle.fromDouble(if index % 2 == 0 then 2.0 / 3 else 4.0 / 3)

  def homeDistance: Double = SpiritOfNaraBot.rangedDistance

  def actionsInPlace(
      gameState: GameState,
      me: Pentagon,
      boss: SpiritOfNara,
      time: Long
  ): Vector[GameAction] = {
    val reactionTime =
      if boss.arcaneExplosionPending then 0L
      else PentagonForSpiritOfNara.reactionTime + index * PentagonForSpiritOfNara.secondDelay

    def maybeDispel = gameState
      .allTBuffs[Chained]
      .filter(buff => time - buff.appearanceTime > reactionTime)
      .toVector
      .sortBy(_.appearanceTime)
      .iterator
      .flatMap(buff => maybeDispelUsage(gameState, time, buff.bearerId, me))
      .nextOption()

    val foe = gameState
      .allTEntities[WaterElemental]
      .values
      .minByOption(_.life)
      .getOrElse(boss)

    maybeDispel.orElse(maybeDamageUsage(gameState, time, me, foe)).toVector
  }

}

object PentagonForSpiritOfNara {
  inline def reactionTime: Long = 1500L
  inline def secondDelay: Long  = 700L
}
