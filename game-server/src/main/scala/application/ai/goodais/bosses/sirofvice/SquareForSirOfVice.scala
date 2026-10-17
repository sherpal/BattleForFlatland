package application.ai.goodais.bosses.sirofvice

import application.ai.goodais.classes.SquareAIController
import gamelogic.entities.Entity
import gamelogic.entities.boss.firstraid.SirOfVice
import gamelogic.entities.classes.Square
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

/** The tank waits for the boss at [[SirOfViceBot.tankPosition]], which keeps the boss near the
  * centre of the room, at equal distance from every pillar.
  */
final case class SquareForSirOfVice(index: Int, entityId: Entity.Id)
    extends SquareAIController(index)
    with SirOfViceBot[Square] {

  def isMelee: Boolean = true

  def restingPosition(
      gameState: GameState,
      me: Square,
      maybeBoss: Option[SirOfVice],
      time: Long
  ): (Complex, Double) = SirOfViceBot.tankPosition -> 10.0

  def actionsInPlace(
      gameState: GameState,
      me: Square,
      boss: SirOfVice,
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
    val maybeHammerHit =
      Option.when(shouldIHammer)(maybeHammerHitUsage(gameState, time, me, boss)).flatten

    maybeHammerHit.orElse(maybeTaunt).orElse(maybeEnrage).toVector
  }

}
