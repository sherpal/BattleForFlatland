package gamelogic.utils

import java.util.concurrent.atomic.AtomicLong

import gamelogic.gamestate.GameAction

final class GameActionIdGenerator(startingId: GameAction.Id)
    extends LongGenerator(GameAction.Id.fromLong) {
  protected val generator: AtomicLong = AtomicLong(startingId.value)
}
