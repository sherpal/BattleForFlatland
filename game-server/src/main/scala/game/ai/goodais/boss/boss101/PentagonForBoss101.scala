package game.ai.goodais.boss.boss101

import game.ai.goodais.GoodAIController
import gamelogic.entities.classes.Pentagon
import scala.reflect.ClassTag
import models.bff.outofgame.gameconfig.PlayerName
import gamelogic.gamestate.{GameAction, GameState}
import models.bff.outofgame.PlayerClasses

import game.ai.utils.maybeAbilityUsage
import gamelogic.entities.boss.Boss101
import gamelogic.entities.movingstuff.PentagonBullet
import gamelogic.abilities.pentagon.CreatePentagonBullet

final class PentagonForBoss101(index: Int) extends GoodAIController[Pentagon] {

  val classTag: ClassTag[Pentagon] = implicitly[ClassTag[Pentagon]]

  val name: PlayerName.AIPlayerName = PlayerName.AIPlayerName(PlayerClasses.Pentagon, index)

  def loop(startingTime: Long, gameState: GameState, me: Pentagon, sendActions: List[GameAction] => Unit): Unit = {
    import gamelogic.physics.Complex.DoubleWithI
    val previousPosition  = me.pos
    val currentPosition   = me.currentPosition(startingTime)
    val travelledDistance = previousPosition distanceTo currentPosition

    val actions: List[GameAction] = gameState.bosses.values.headOption match {
      case Some(theBoss) =>
        val rotationTowardsBoss = (theBoss.pos - me.pos).arg

        val maybePentagonBullet = maybeAbilityUsage(
          me,
          CreatePentagonBullet(
            0L,
            startingTime,
            me.id,
            me.pos,
            CreatePentagonBullet.damage,
            rotationTowardsBoss,
            me.colour
          ),
          gameState
        ).startCasting

        maybePentagonBullet.toList
      case None =>
        val targetPosition = Boss101.bossStartingPosition + (index - 0.5) * 150
        preGameMovement(startingTime, me, currentPosition, targetPosition, travelledDistance)
    }

    sendActions(actions)

  }

}
