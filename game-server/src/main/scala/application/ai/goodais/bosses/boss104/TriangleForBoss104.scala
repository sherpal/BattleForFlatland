package application.ai.goodais.bosses.boss104

import application.ai.goodais.classes.TriangleAIController
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.abilities.triangle.Cut
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss104.{BigGuy, BigGuyDeathMark}
import gamelogic.entities.classes.{Constants, Triangle}
import gamelogic.gamestate.gameactions.MovingBodyMoves
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

/** Bot for the Triangle of Boss104
  *
  * It has to:
  *
  *   - take care of the big guy
  *   - pull them and put them in a safe spot to form a good triangle
  *   - cut the big guy ability when it casts it
  *   - have the common behaviour
  */
final case class TriangleForBoss104(index: Int, entityId: Entity.Id)
    extends TriangleAIController(index) {
  override protected def takeActions(
      currentGameState: GameState,
      me: Triangle,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      obstacleGraph: Graph
  ): Vector[GameAction] =
    handleBigGuy(me, currentGameState, startTime, timeSinceLastFrame).getOrElse {
      // todo

      Vector.empty
    }

  def handleBigGuy(
      me: Triangle,
      gameState: GameState,
      time: Long,
      timeSinceLastFrame: Long
  ): Option[Vector[GameAction]] =
    gameState.allTEntities[BigGuy].minByOption(_._1).map { (bigGuyId, bigGuy) =>
      def maybeCutBigGuy = for {
        castingInfo <- gameState.castingEntityInfo.get(bigGuyId)
        if castingInfo.isCasting(time, 0L)
        if castingInfo.remainingCastingTime(time, 0L) < (castingInfo.castingTime / 2)
        abilityCandidate = Cut(Ability.UseId.dummy, time, me.id, bigGuyId)
        ability <- application.ai.utils.maybeAbilityUsage(me, abilityCandidate, gameState)
      } yield ability.toStartCasting(time)

      val iHaveAggro = bigGuy.targetId == me.id

      val currentNumberOfDeathMarks = gameState.allTEntities[BigGuyDeathMark].size // 0, 1 or 2
      val deathTargetPosition =
        Complex.polar(
          Constants.bossRadius * 3,
          currentNumberOfDeathMarks * 2 * math.Pi / 3
        )

      val (previousPosition, currentPosition, travelledDistance) = someDistanceInfo(time, me)

      val maybePlaceBigGuy =
        Option
          .when(iHaveAggro && currentPosition.distanceTo(deathTargetPosition) > 10) {
            preGameMovement(
              time,
              me,
              currentPosition,
              targetPosition = deathTargetPosition,
              travelledDistance
            )
          }

      val maybeGoToBigGuy =
        Option.when(
          !iHaveAggro && currentPosition
            .distanceTo(bigGuy.currentPosition(time)) > WithTargetAbility.meleeRange
        ) {
          preGameMovement(
            time,
            me,
            currentPosition,
            bigGuy.currentPosition(time),
            travelledDistance
          )
        }

      val maybeAttackBigGuy = defaultAggressiveAbility(gameState, me, time, bigGuy)
        .orElse(maybeEnergyKickUsage(gameState, time, me, bigGuy))

      val movement = maybeGoToBigGuy
        .orElse(maybePlaceBigGuy)
        .orElse(stopMoving(time, me, currentPosition, 0).map(Vector(_)))
        .getOrElse(Vector.empty)

      movement ++ maybeCutBigGuy
        .map(Vector(_))
        .orElse(maybeAttackBigGuy.map(Vector(_)))
        .getOrElse(Vector.empty)
    }
}
