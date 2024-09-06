package gamelogic.utils

import java.util.concurrent.atomic.AtomicLong

import gamelogic.buffs.Buff

final class BuffIdGenerator(startingId: Buff.Id) extends LongGenerator[Buff.Id](Buff.Id.fromLong) {
  protected val generator: AtomicLong = AtomicLong(startingId.value)
}
