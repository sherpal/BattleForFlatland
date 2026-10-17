package application.ai.goodais.bosses.sirofvice

import application.ai.goodais.classes.HexagonAIController
import gamelogic.buffs.boss.sirofvice.Inflamed
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.firstraid.SirOfVice
import gamelogic.entities.classes.Hexagon
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

/** The healer stays out of the aura (which silences it), on the side players come from. */
final case class HexagonForSirOfVice(index: Int, entityId: Entity.Id)
    extends HexagonAIController(index)
    with SirOfViceBot[Hexagon] {

  def isMelee: Boolean = false

  private val direction: Angle = if index % 2 == 0 then Angle.Pi else Angle.zero

  def restingPosition(
      gameState: GameState,
      me: Hexagon,
      maybeBoss: Option[SirOfVice],
      time: Long
  ): (Complex, Double) = rangedSpot(direction, maybeBoss, time) -> 10.0

  def actionsInPlace(
      gameState: GameState,
      me: Hexagon,
      boss: SirOfVice,
      time: Long
  ): Vector[GameAction] =
    if isSilenced(gameState, me) then Vector.empty
    else {
      val playersByLifeRatio =
        gameState.players.values.toVector.sortBy(player => player.life / player.maxLife)

      def maybeFlashHeal = playersByLifeRatio.iterator
        .filter(player => player.life < player.maxLife * 0.5)
        .flatMap(maybeFlashHealUsage(gameState, time, _, me))
        .nextOption()

      val inflamed = gameState.allTBuffs[Inflamed].map(_.bearerId).toSet

      def maybeHot = putHotOnFirstDefined(
        gameState,
        me,
        time,
        maybeTankWithNotEnoughHot(gameState, me) +: playersByLifeRatio.map(player =>
          Option.when(
            countOfMyHotOnEntity(gameState, player.id, me) == 0 &&
              (inflamed.contains(player.id) || player.life < player.maxLife * 0.8)
          )(player)
        )
      )

      maybeFlashHeal.orElse(maybeHot).toVector
    }

}
