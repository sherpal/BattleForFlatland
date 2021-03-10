package game.ai.goodais.boss.boss101

import game.ai.goodais.GoodAIController
import gamelogic.entities.classes.Hexagon
import scala.reflect.ClassTag
import models.bff.outofgame.gameconfig.PlayerName
import gamelogic.gamestate.{GameAction, GameState}
import models.bff.outofgame.PlayerClasses
import gamelogic.entities.boss.Boss101
import game.ai.utils.maybeAbilityUsage
import gamelogic.abilities.hexagon.HexagonHot
import gamelogic.entities.classes.Square
import gamelogic.abilities.boss.boss101.BigDot
import gamelogic.buffs.Buff
import gamelogic.buffs.DoT
import gamelogic.entities.Entity
import gamelogic.buffs.HoT
import gamelogic.abilities.hexagon.FlashHeal

final class HealForBoss101(index: Int) extends GoodAIController[Hexagon] {

  val classTag: ClassTag[Hexagon] = implicitly[ClassTag[Hexagon]]

  val name: PlayerName.AIPlayerName = PlayerName.AIPlayerName(PlayerClasses.Hexagon, index)

  def loop(startingTime: Long, gameState: GameState, me: Hexagon, sendActions: List[GameAction] => Unit): Unit = {
    import gamelogic.physics.Complex.DoubleWithI
    val previousPosition  = me.pos
    val currentPosition   = me.currentPosition(startingTime)
    val travelledDistance = previousPosition distanceTo currentPosition

    val actions: List[GameAction] = gameState.bosses.values.headOption match {
      case Some(theBoss) =>
        def countOfMyHotOnEntity(entityId: Entity.Id): Int =
          gameState
            .allBuffsOfEntity(entityId)
            .collect {
              case hot: HoT => hot
            }
            .count(hot => hot.sourceId == me.id && hot.resourceIdentifier == Buff.hexagonHotIdentifier)

        val maybeTank = gameState.players.values
          .collectFirst {
            case player: Square => player
          }
          .filter(square => countOfMyHotOnEntity(square.id) == 0)

        val entityWithBossDebuffAndNoHotFromMe = gameState.players.values.find { player =>
          val buffs = gameState.allBuffsOfEntity(player.id)
          buffs.count(_.resourceIdentifier == Buff.boss101BigDotIdentifier) > countOfMyHotOnEntity(player.id)
        }

        val putHot = (for {
          target       <- maybeTank.orElse(entityWithBossDebuffAndNoHotFromMe)
          abilityUsage <- maybeAbilityUsage(me, HexagonHot(0L, startingTime, me.id, target.id), gameState)
        } yield abilityUsage).startCasting

        val maybeFlashHeal = for {
          target  <- gameState.players.values.find(buddy => buddy.life < buddy.maxLife / 2)
          ability <- maybeAbilityUsage(me, FlashHeal(0L, startingTime, me.id, target.id), gameState).startCasting
        } yield ability

        putHot.orElse(maybeFlashHeal).toList

      case None =>
        val targetPosition = Boss101.bossStartingPosition - 100.i + (index - 0.5) * 60
        preGameMovement(startingTime, me, currentPosition, targetPosition, travelledDistance)
    }

    sendActions(actions)

  }

}
