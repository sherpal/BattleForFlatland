package application.ai.boss.spiritofnaraunits

import application.ai.AIController
import application.ai.utils.maybeAbilityUsage
import gamelogic.abilities.Ability
import gamelogic.abilities.boss.spiritofnara.Waterbolt
import gamelogic.entities.Entity.Id
import gamelogic.entities.boss.spiritofnara.WaterElemental
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.gameactions.spiritofnara.AddWaterElemental
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

import scala.util.Random

/** The brain of a [[WaterElemental]], registered in `AIManager` on the [[AddWaterElemental]]
  * action. It stays where it was summoned, and throws Waterbolts at random players.
  */
class WaterElementalController extends AIController[WaterElemental, AddWaterElemental] {

  override protected def takeActions(
      currentGameState: GameState,
      me: WaterElemental,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      maybeTarget: Option[PlayerClass],
      obstacleGraph: Graph
  ): Vector[GameAction] =
    Random
      .shuffle(currentGameState.players.values.toVector)
      .headOption
      .flatMap(victim =>
        maybeAbilityUsage(
          me,
          Waterbolt(Ability.UseId.dummy, startTime, me.id, victim.id),
          currentGameState
        )
      )
      .map(_.toStartCasting(startTime))
      .toVector

  override protected def getMe(gameState: GameState, entityId: Id): Option[WaterElemental] =
    gameState.entityByIdAs[WaterElemental](entityId)
}
