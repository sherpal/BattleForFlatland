package application.ai.goodais.classes

import application.ai.goodais.GoodAIController
import gamelogic.entities.classes.Hexagon
import gamelogic.gamestate.GameState
import gamelogic.entities.Entity
import gamelogic.buffs.HoT
import gamelogic.buffs.Buff
import gamelogic.abilities.hexagon.FlashHeal
import application.ai.utils.maybeAbilityUsage
import gamelogic.entities.classes.Square
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.abilities.hexagon.HexagonHot
import scala.reflect.ClassTag
import gamelogic.abilities.Ability
import models.bff.outofgame.gameconfig.PlayerName
import models.bff.outofgame.PlayerClasses

trait HexagonAIController(index: Int) extends GoodAIController[Hexagon] {

  final val classTag: ClassTag[Hexagon] = implicitly[ClassTag[Hexagon]]

  val name = PlayerName.AIPlayerName(PlayerClasses.Hexagon, index)

  def countOfMyHotOnEntity(gameState: GameState, targetEntityId: Entity.Id, me: Hexagon): Int =
    gameState
      .allBuffsOfEntity(targetEntityId)
      .collect { case hot: HoT =>
        hot
      }
      .count(hot => hot.sourceId == me.id && hot.resourceIdentifier == Buff.hexagonHotIdentifier)

  /** Returns maybe a [[FlashHeal]] cast action if the life of one unit is smaller than its max life
    * times the threshold.
    *
    * For example, if threshold = 0.5, it will cast [[FlashHeal]] on the first player it finds with
    * its life less than its max life divided by 2.
    *
    * @param gameState
    *   current [[GameState]]
    * @param time
    *   time for casting the ability
    * @param me
    *   instance of the [[Hexagon]]
    * @param threshold
    *   desired threshold to apply (between 0 and 1, otherwise quite useless.)
    */
  def maybeFlashHealWithThreshold(
      gameState: GameState,
      time: Long,
      me: Hexagon,
      threshold: Double
  ): Option[EntityStartsCasting] =
    for {
      target <- gameState.players.values.find(buddy => buddy.life < buddy.maxLife * threshold)
      ability <- maybeAbilityUsage(
        me,
        FlashHeal(Ability.UseId.dummy, time, me.id, target.id),
        gameState
      ).startCasting
    } yield ability

  /** Returns the tank with the minimum count of my HoT on it, but only if it satisfy the filtering,
    * and only if the number of HoT on it is smaller than the requiredCount.
    *
    * An example of filtering could be if I only have to take care on one tank, and not another one,
    * or I want to filter only on a [[Square]] which is in sight.
    *
    * If the required count is 1, I will only put one hot on the tank.
    */
  def maybeTankWithNotEnoughHot(
      gameState: GameState,
      me: Hexagon,
      filtering: Square => Boolean = (_: Square) => true,
      requiredCount: Int = 1
  ): Option[Square] =
    gameState.players.values
      .collect { case square: Square => square }
      .filter(filtering)
      .map(square => (square, countOfMyHotOnEntity(gameState, square.id, me)))
      .minByOption(_._2)
      .filter(_._2 < requiredCount)
      .map(_._1)

  /** Casts a [[HexagonHot]] on the first entity defined in the list of entities such that it is
    * legal to cast [[HexagonHot]] on it. Returns None if no such entity exists.
    */
  def putHotOnFirstDefined(
      gameState: GameState,
      me: Hexagon,
      time: Long,
      entities: List[Option[Entity]]
  ): Option[EntityStartsCasting] =
    entities
      .map(maybeEntity =>
        maybeEntity.flatMap(target =>
          maybeAbilityUsage(
            me,
            HexagonHot(Ability.UseId.dummy, time, me.id, target.id),
            gameState
          ).startCasting
        )
      )
      .collectFirst { case Some(cast) => cast }

}
