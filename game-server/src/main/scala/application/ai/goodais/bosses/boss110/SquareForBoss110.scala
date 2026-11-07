package application.ai.goodais.bosses.boss110

import application.ai.goodais.classes.SquareAIController
import gamelogic.abilities.WithTargetAbility
import gamelogic.abilities.boss.boss110.{SpawnBigGuies, SpawnSmallGuies}
import gamelogic.abilities.square.Cleave
import gamelogic.buffs.boss.boss110.BrokenArmor
import gamelogic.entities.{Entity, LivingEntity, MovingBody, WithTarget}
import gamelogic.entities.boss.boss110.SmallGuy
import gamelogic.entities.classes.Square
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

/** The two tanks of Boss110.
  *
  * The first one (index 0) stands on the right, where the big guys spawn. It tanks the boss and
  * the big guys. The second one (index 1) stands on the left, where the small guys spawn, and keeps
  * all of them on it: since the creeping shadow follows the small guys, that keeps the shadow away
  * from the boss.
  */
final case class SquareForBoss110(index: Int, entityId: Entity.Id)
    extends SquareAIController(index)
    with Boss110Bot[Square] {

  private def tanksTheBoss: Boolean = index == 0

  def restingPosition(gameState: GameState, me: Square, time: Long): (Complex, Double) =
    if tanksTheBoss then
      Complex(SpawnBigGuies.bigGuiesPositions.map(_.re).max - 80, 0) -> 10.0
    else
      (SpawnSmallGuies.startingPositions.sum / SpawnSmallGuies.startingPositions.length) -> 10.0

  def actionsInPlace(gameState: GameState, me: Square, time: Long): Vector[GameAction] =
    if tanksTheBoss then tankTheBossAndBigGuys(gameState, me, time)
    else tankTheSmallGuys(gameState, me, time)

  private def tankTheBossAndBigGuys(
      gameState: GameState,
      me: Square,
      time: Long
  ): Vector[GameAction] = {
    val foes: Vector[LivingEntity & MovingBody & WithTarget] =
      gameState.bosses.values.toVector ++ Boss110Bot.bigGuys(gameState)
    val target = Boss110Bot.bigGuyToKill(gameState).orElse(gameState.bosses.values.headOption)

    // Enrage makes us take more damage, which is a bad idea with a broken armour
    val maybeEnrage = maybeEnrageUsage(
      gameState,
      time,
      me,
      me.resourceAmount.amount < 5 && me.life > 180 && !alreadyEnraged(gameState, me) &&
        !gameState.hasBuffOfType[BrokenArmor](me.id)
    )

    maybeBraceUsage(gameState, time, me)
      .orElse(tauntTheFirstFoeNotOnMe(gameState, time, me, foes))
      .orElse(target.flatMap(maybeHammerHitUsage(gameState, time, me, _)))
      .orElse(maybeEnrage)
      .toVector
  }

  private def tankTheSmallGuys(gameState: GameState, me: Square, time: Long): Vector[GameAction] = {
    val myPosition = me.currentPosition(time)
    val smallGuys  = gameState.allTEntities[SmallGuy].values.toVector
    val inReach = smallGuys.filter(
      _.currentPosition(time).distanceTo(myPosition) < Cleave.coneHeight + SmallGuy.shape.radius
    )

    // towards the middle of the small guys we can reach, which hits most of them
    val maybeCleave = Option
      .when(inReach.nonEmpty)(
        (inReach.map(_.currentPosition(time)).sum / inReach.length - myPosition).arg
      )
      .flatMap(maybeCleaveUsage(gameState, time, me, _))

    val maybeHammerHit = smallGuys
      .filter(_.currentPosition(time).distanceTo(myPosition) < WithTargetAbility.meleeRange)
      .minByOption(_.life)
      .flatMap(maybeHammerHitUsage(gameState, time, me, _))

    val maybeEnrage = maybeEnrageUsage(
      gameState,
      time,
      me,
      me.resourceAmount.amount < 5 && me.life > 180 && !alreadyEnraged(gameState, me)
    )

    maybeBraceUsage(gameState, time, me)
      .orElse(tauntTheFirstFoeNotOnMe(gameState, time, me, smallGuys))
      .orElse(maybeCleave)
      .orElse(maybeHammerHit)
      .orElse(maybeEnrage)
      .toVector
  }

  private def tauntTheFirstFoeNotOnMe(
      gameState: GameState,
      time: Long,
      me: Square,
      foes: Vector[LivingEntity & MovingBody & WithTarget]
  ) =
    foes.iterator
      .filter(_.targetId != me.id)
      .flatMap(maybeTauntUsage(gameState, time, me, _))
      .nextOption()

}
