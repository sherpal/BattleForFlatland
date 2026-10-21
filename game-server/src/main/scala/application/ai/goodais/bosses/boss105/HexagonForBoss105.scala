package application.ai.goodais.bosses.boss105

import application.ai.goodais.classes.HexagonAIController
import gamelogic.entities.Entity
import gamelogic.entities.classes.Hexagon
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

/** The healer stays in the middle of the room, from where both tanks are in range. */
final case class HexagonForBoss105(index: Int, entityId: Entity.Id)
    extends HexagonAIController(index)
    with Boss105Bot[Hexagon] {

  def restingPosition(gameState: GameState, me: Hexagon, time: Long): (Complex, Double) =
    (Boss105Bot.healerPosition + Complex(0, 40 * index)) -> 10.0

  override def instantActions(
      gameState: GameState,
      me: Hexagon,
      time: Long
  ): Vector[GameAction] = {
    val playersByLifeRatio =
      gameState.players.values.toVector.sortBy(player => player.life / player.maxLife)

    putHotOnFirstDefined(
      gameState,
      me,
      time,
      maybeTankWithNotEnoughHot(gameState, me) +: playersByLifeRatio.map(player =>
        Option.when(
          countOfMyHotOnEntity(gameState, player.id, me) == 0 && player.life < player.maxLife * 0.7
        )(player)
      )
    ).toVector
  }

  def actionsInPlace(gameState: GameState, me: Hexagon, time: Long): Vector[GameAction] =
    gameState.players.values.toVector
      .sortBy(player => player.life / player.maxLife)
      .iterator
      .filter(player => player.life < player.maxLife * 0.55)
      .flatMap(maybeFlashHealUsage(gameState, time, _, me))
      .nextOption()
      .toVector

}
