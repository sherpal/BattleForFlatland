package gamelogic.utils

import java.util.concurrent.atomic.AtomicLong

import gamelogic.buffs.Buff

final class BuffIdGenerator(startingId: Buff.Id) extends LongGenerator {
  protected val generator: AtomicLong = new AtomicLong(startingId)
}
