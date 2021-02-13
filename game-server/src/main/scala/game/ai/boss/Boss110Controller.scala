package game.ai.boss
import game.ai.utils._
import gamelogic.entities.Entity.Id
import gamelogic.entities.boss.dawnoftime.Boss110
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.gameactions.{EntityStartsCasting, SpawnBoss}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

import scala.util.Random
import gamelogic.abilities.boss.boss110.SpawnBigGuies
import gamelogic.abilities.boss.boss110.PlaceBombPods
import gamelogic.entities.boss.boss110.BombPod
import gamelogic.abilities.boss.boss110.ExplodeBombs
import gamelogic.abilities.Ability
import scala.reflect.ClassTag

object Boss110Controller extends SimpleAIController[Boss110, SpawnBoss] {

  def meleeRange: Double = Boss110.meleeRange

  def fullSpeed: Double = Boss110.fullSpeed

  def actions(gameState: GameState, me: Boss110, time: Long): List[Option[EntityStartsCasting]] = {
    def iMightCastThis[T <: Ability](ability: T) = maybeAbilityUsage(me, ability, gameState)

    val maybeSpawnBigGuies = Some(SpawnBigGuies(0L, time, me.id))
      .filter(me.canUseAbilityBoolean(_, time))
      .startCasting

    val maybePlaceBombPods = iMightCastThis(
      PlaceBombPods(0L, time, me.id, Nil) // Nil to not compute for nothing
    ).map(
        _.copy(
          positions = PlaceBombPods
            .randomPositionsInSquare(
              -Boss110.halfWidth * 9 / 10 / 2,
              Boss110.halfHeight * 9 / 10 / 2,
              PlaceBombPods.numberOfBombs
            )
        )
      )
      .startCasting

    val maybeExplodeBombs = iMightCastThis(ExplodeBombs(0L, time, me.id)).startCasting

    List(
      maybeSpawnBigGuies,
      maybePlaceBombPods,
      maybeExplodeBombs,
      me.maybeAutoAttack(time, gameState).startCasting
    )
  }

  val classTag: ClassTag[Boss110] = implicitly[ClassTag[Boss110]]

}
