package application.ai.goodais.bosses.boss110

import application.ai.goodais.classes.HexagonAIController
import gamelogic.entities.Entity
import gamelogic.entities.boss.dawnoftime.Boss110
import gamelogic.entities.classes.{Hexagon, Square}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

/** The Hexagons of Boss110 heal from the middle of the room, from where they reach both tanks. Mana
  * is scarce, so each of them keeps a HoT on the tank of its side only (the first on the right, the
  * second on the left), and they flash heal whoever is the most hurt.
  */
final case class HexagonForBoss110(index: Int, entityId: Entity.Id)
    extends HexagonAIController(index)
    with Boss110Bot[Hexagon] {

  def restingPosition(gameState: GameState, me: Hexagon, time: Long): (Complex, Double) =
    Complex(0, (index - 0.5) * Boss110.halfHeight) -> 10.0

  override def instantActions(gameState: GameState, me: Hexagon, time: Long): Vector[GameAction] =
    putHotOnFirstDefined(
      gameState,
      me,
      time,
      Some(maybeTankWithNotEnoughHot(gameState, me, isTankOfMySide(_, time)))
    )
      .orElse(putHotOnFirstWithThreshold(gameState, me, time, 0.7))
      .toVector

  private def isTankOfMySide(square: Square, time: Long): Boolean =
    (square.currentPosition(time).re > 0) == (index == 0)

  def actionsInPlace(gameState: GameState, me: Hexagon, time: Long): Vector[GameAction] =
    gameState.players.values
      .filter(player => player.life < player.maxLife * 0.6)
      .minByOption(player => player.life / player.maxLife)
      .flatMap(maybeFlashHealUsage(gameState, time, _, me))
      .toVector

}
