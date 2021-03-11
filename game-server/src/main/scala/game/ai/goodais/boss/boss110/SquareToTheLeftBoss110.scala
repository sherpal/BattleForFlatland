package game.ai.goodais.boss.boss110

import game.ai.goodais.classes.SquareAIController
import models.bff.outofgame.gameconfig.PlayerName
import gamelogic.entities.classes.Square
import gamelogic.gamestate.{GameAction, GameState}
import models.bff.outofgame.PlayerClasses
import gamelogic.entities.boss.boss110.SmallGuy
import gamelogic.abilities.boss.boss110.SpawnSmallGuies
import scala.util.Random

/**
  * This is the implementation for the [[Square]] AI of Boss110 which will go to the left part
  * of the game.
  * This tank is responsible for collecting small units, and keeping them on him.
  *
  * It will have index 0
  */
object SquareToTheLeftBoss110 extends SquareAIController {

  val name: PlayerName.AIPlayerName = PlayerName.AIPlayerName(PlayerClasses.Square, 0)

  def loop(startingTime: Long, gameState: GameState, me: Square, sendActions: List[GameAction] => Unit): Unit = {
    val actions: List[GameAction] = gameState.bosses.values.headOption match {
      case Some(_) =>
        val smallGuies = gameState.allTEntities[SmallGuy].values
        val maybeTaunt =
          for {
            // finding small guy not targetting me, or for which I have no threat
            target <- smallGuies
              .find(_.targetId != me.id)
              .orElse(smallGuies.find(_.damageThreats.getOrElse(me.id, 0.0) == 0.0))
            taunt <- maybeTauntUsage(gameState, startingTime, me, target)
          } yield taunt

        lazy val (smallGuiesAbove, smallGuiesBelow) = smallGuies.partition(_.pos.im > 0)

        lazy val maybeCleave = for {
          isClusterAbove <- Some(Random.nextBoolean())
          targetPosition = if (isClusterAbove) smallGuiesAbove.map(_.pos).avg else smallGuiesBelow.map(_.pos).avg
          direction      = (targetPosition - me.pos).arg
          cleave <- maybeCleaveUsage(gameState, startingTime, me, direction)
        } yield cleave

        maybeTaunt.orElse(maybeCleave).toList
      case None =>
        val targetPosition                      = SpawnSmallGuies.startingPositions.avg
        val (previousPosition, currentPosition) = someDistanceInfo(startingTime, me)
        preGameMovement(startingTime, me, currentPosition, targetPosition, previousPosition distanceTo currentPosition)
    }

    sendActions(actions)

  }

}
