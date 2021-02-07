package gamelogic.gamestate.gameactions.boss110

import gamelogic.entities.Entity
import gamelogic.physics.Complex
import gamelogic.gamestate.GameAction

final case class AddBigGuies(id: GameAction.Id, time: Long, idsAndPositions: List[(Entity.Id, Complex)])
    extends GameAction
