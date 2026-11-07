package gamelogic.abilities.boss.boss110

import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import gamelogic.entities.Resource
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.boss.dawnoftime.Boss110
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.gameactions.UseAbility
import gamelogic.gamestate.gameactions.boss110.AddBombPods
import gamelogic.physics.Complex
import scala.util.Random
import gamelogic.entities.boss.boss110.BombPod
import scala.annotation.tailrec
import services.i18n.I18n

/** Puts [[PlaceBombPods.numberOfBombs]] [[gamelogic.entities.boss.boss110.BombPod]]s at random
  * positions on the board. It also make the caster "use" the ability that explodes the bombs, so
  * that it will trigger the actual explosion [[ExplodeBombs.cooldown]] after that.
  */
final case class PlaceBombPods(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id
) extends Ability
    with AbilityInfoFromMetadata[PlaceBombPods.type] {

  def metadata = PlaceBombPods

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0.0, Resource.NoResource)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(
      // Fake the use of ExplodeBombs so that the boss will use it some time after.
      UseAbility(
        genActionId(),
        time,
        casterId,
        genAbilityUseId(),
        ExplodeBombs(genAbilityUseId(), time, casterId)
      ),
      AddBombPods(
        genActionId(),
        time,
        PlaceBombPods
          .randomPositionsInSquare(
            PlaceBombPods.bombsAreaCenter,
            PlaceBombPods.bombsAreaRadius,
            PlaceBombPods.numberOfBombs
          )
          .map(genEntityId() -> _),
        casterId
      )
    )

  def copyWithNewTimeAndId(newTime: Long, newId: Ability.UseId): Ability =
    copy(useId = newId, time = newTime)

  def canBeCast(gameState: GameState, time: Long)(using I18n): Option[String] = Option.empty

}

object PlaceBombPods extends AbilityMetadata {

  def name: String = "Place Bomb Pods"

  def cooldown: Long = 20000L

  def castingTime: Long = 1500L

  def timeToFirstAbility: Long = 5000L

  val numberOfBombs: Int = 4

  /** The bombs are placed in a square between the small guys and the middle of the room. */
  def bombsAreaCenter: Complex = Complex(-Boss110.halfWidth * 9.0 / 20, 0)
  def bombsAreaRadius: Double  = Boss110.halfHeight * 9.0 / 20

  /** A player covers a bomb when they are less than `BombPod.shape.radius + playerRadius` apart.
    * With bombs further apart than twice that, nobody can cover two bombs at once. We take some
    * margin, so that a player standing next to their bomb does not brush against another one.
    */
  def minimumDistanceBetweenBombs: Double =
    3 * (BombPod.shape.radius + Constants.playerRadius)

  /** Returns random position in a square of given center and L-Infinity radius, that are apart each
    * other with a distance at least [[minimumDistanceBetweenBombs]].
    *
    * While finding random position, we will need to make retries. We limit to 500 retries, so that
    * we can't end up in an infinite loop (for example, if you want to generate to much positions
    * for the size of the square.)
    *
    * @param center
    *   center of the square in which positions are generated
    * @param lInfinityRadius
    *   L-infinity radius of the square
    * @param numberOfPositions
    *   number of positions to generate
    * @return
    *   numberOfPositions random positions in the square, or less if the algorithm had to retry too
    *   much.
    */
  def randomPositionsInSquare(
      center: Complex,
      lInfinityRadius: Double,
      numberOfPositions: Int
  ): List[Complex] = {
    def randomInRange() = Random.between(-lInfinityRadius, lInfinityRadius)

    @tailrec
    def positionsAccumulator(
        currentPositions: List[Complex],
        remainingToFind: Int,
        triesLeft: Int
    ): List[Complex] =
      if (remainingToFind == 0 || triesLeft == 0) currentPositions
      else {
        val newRandomPosition = center + Complex(randomInRange(), randomInRange())

        if (
          currentPositions.forall(_.distanceTo(newRandomPosition) > minimumDistanceBetweenBombs)
        )
          positionsAccumulator(
            newRandomPosition +: currentPositions,
            remainingToFind - 1,
            triesLeft
          )
        else positionsAccumulator(currentPositions, remainingToFind, triesLeft - 1)
      }

    positionsAccumulator(Nil, numberOfPositions, 500)
  }

  def abilityId: Ability.AbilityId = Ability.boss110PlaceBombPods

}
