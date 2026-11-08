package application.ai.goodais.bosses.spiritofnara

import application.ai.goodais.classes.HexagonAIController
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.firstraid.SpiritOfNara
import gamelogic.entities.classes.Hexagon
import gamelogic.gamestate.{GameAction, GameState}

/** The healer stands on the side players come from, out of reach of the Counterspell. */
final case class HexagonForSpiritOfNara(index: Int, entityId: Entity.Id)
    extends HexagonAIController(index)
    with SpiritOfNaraBot[Hexagon] {

  def homeAngle: Angle = Angle.Pi + Angle.fromDouble(0.25 * index)

  def homeDistance: Double = SpiritOfNaraBot.rangedDistance

  def actionsInPlace(
      gameState: GameState,
      me: Hexagon,
      boss: SpiritOfNara,
      time: Long
  ): Vector[GameAction] = {
    val playersByLifeRatio =
      gameState.players.values.toVector.sortBy(player => player.life / player.maxLife)

    def maybeFlashHeal = playersByLifeRatio.iterator
      .filter(player => player.life < player.maxLife * 0.5)
      .flatMap(maybeFlashHealUsage(gameState, time, _, me))
      .nextOption()

    def maybeHot = putHotOnFirstDefined(
      gameState,
      me,
      time,
      maybeTankWithNotEnoughHot(gameState, me) +: playersByLifeRatio.map(player =>
        Option.when(
          countOfMyHotOnEntity(gameState, player.id, me) == 0 && player.life < player.maxLife * 0.8
        )(player)
      )
    )

    maybeFlashHeal.orElse(maybeHot).toVector
  }

}
