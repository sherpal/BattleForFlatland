package gamelogic.utils

import java.util.concurrent.atomic.AtomicLong

import gamelogic.gamestate.GameAction

final class GameActionIdGenerator(startingId: GameAction.Id) extends LongGenerator {
  protected val generator: AtomicLong = new AtomicLong(startingId)
}
