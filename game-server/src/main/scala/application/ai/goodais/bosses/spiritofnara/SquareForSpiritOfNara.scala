package application.ai.goodais.bosses.spiritofnara

import application.ai.goodais.classes.SquareAIController
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.firstraid.SpiritOfNara
import gamelogic.entities.classes.Square
import gamelogic.gamestate.{GameAction, GameState}

/** The tank stands right next to the boss, on the side players come from. The boss hardly hits in
  * melee, so this is mostly about keeping its threat and helping with the damage.
  */
final case class SquareForSpiritOfNara(index: Int, entityId: Entity.Id)
    extends SquareAIController(index)
    with SpiritOfNaraBot[Square] {

  def homeAngle: Angle = Angle.Pi

  def homeDistance: Double = SpiritOfNaraBot.meleeDistance

  def actionsInPlace(
      gameState: GameState,
      me: Square,
      boss: SpiritOfNara,
      time: Long
  ): Vector[GameAction] = {
    val shouldIHammer = isMyThreatTowardsTargetEnough(boss, me)

    val maybeTaunt =
      Option.unless(shouldIHammer)(maybeTauntUsage(gameState, time, me, boss)).flatten
    val maybeEnrage = maybeEnrageUsage(
      gameState,
      time,
      me,
      me.resourceAmount.amount < 5 && me.life > 100 && !alreadyEnraged(gameState, me)
    )

    maybeBraceUsage(gameState, time, me)
      .orElse(maybeHammerHitUsage(gameState, time, me, boss))
      .orElse(maybeTaunt)
      .orElse(maybeEnrage)
      .toVector
  }

}
