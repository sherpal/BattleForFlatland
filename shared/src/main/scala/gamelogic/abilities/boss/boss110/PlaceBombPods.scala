package gamelogic.abilities.boss.boss110

import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import gamelogic.entities.Resource
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import gamelogic.gamestate.GameState
import gamelogic.docs.AbilityMetadata
import gamelogic.gamestate.gameactions.UseAbility
import gamelogic.gamestate.gameactions.boss110.AddBombPods
import gamelogic.physics.Complex
import scala.util.Random
import gamelogic.entities.boss.boss110.BombPod
import scala.annotation.tailrec

/**
  * Puts on the board one [[gamelogic.entities.boss.boss110.BombPod]] for each of the
  * specified positions. It also make the caster "use" the ability that exploses the bombs, so
  * that it will trigger the actual explosion the given time after that.
  */
final case class PlaceBombPods(useId: Ability.UseId, time: Long, casterId: Entity.Id, positions: List[Complex])
    extends Ability {

  def abilityId: Ability.AbilityId = Ability.boss110PlaceBombPods

  def cooldown: Long = PlaceBombPods.cooldown

  def castingTime: Long = PlaceBombPods.castingTime

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0.0, Resource.NoResource)

  def createActions(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    List(
      // Fake the use of ExplodeBombs so that the boss will use it some time after.
      UseAbility(
        idGeneratorContainer.gameActionIdGenerator(),
        time,
        casterId,
        idGeneratorContainer.abilityUseIdGenerator(),
        ExplodeBombs(idGeneratorContainer.abilityUseIdGenerator(), time, casterId)
      ),
      AddBombPods(
        idGeneratorContainer.gameActionIdGenerator(),
        time,
        positions.map(idGeneratorContainer.entityIdGenerator() -> _),
        casterId
      )
    )

  def copyWithNewTimeAndId(newTime: Long, newId: Ability.UseId): Ability =
    copy(useId = newId, time = newTime)

  def canBeCast(gameState: GameState, time: Long): Option[String] = Option.empty

}

object PlaceBombPods extends AbilityMetadata {

  def name: String = "Place Bomb Pods"

  def cooldown: Long = 20000L

  def castingTime: Long = 1500L

  def timeToFirstAbility: Long = 5000L

  val numberOfBombs: Int = 4

  /**
    * Returns random position in a square of given center and L-Infinity radius, that are
    * apart each other with a distance at least twice the radius of [[BombPod]]s.
    *
    * While finding random position, we will need to make retries. We limit to 500 retries,
    * so that we can't end up in an infinite loop (for example, if you want to generate to much positions
    * for the size of the square.)
    *
    * @param center center of the square in which positions are generated
    * @param lInfinityRadius L-infinity radius of the square
    * @param numberOfPositions number of positions to generate
    * @return numberOfPositions random positions in the square, or less if the algorithm had to retry too much.
    */
  def randomPositionsInSquare(center: Complex, lInfinityRadius: Double, numberOfPositions: Int): List[Complex] = {
    def randomInRange() = Random.between(-lInfinityRadius, lInfinityRadius)

    @tailrec
    def positionsAccumulator(currentPositions: List[Complex], remainingToFind: Int, triesLeft: Int): List[Complex] =
      if (remainingToFind == 0 || triesLeft == 0) currentPositions
      else {
        val newRandomPosition = center + Complex(randomInRange(), randomInRange())

        if (currentPositions.forall(_.distanceTo(newRandomPosition) > BombPod.shape.radius * 2))
          positionsAccumulator(newRandomPosition +: currentPositions, remainingToFind - 1, triesLeft)
        else positionsAccumulator(currentPositions, remainingToFind, triesLeft - 1)
      }

    positionsAccumulator(Nil, numberOfPositions, 500)
  }

}
