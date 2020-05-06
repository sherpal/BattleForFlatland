package gamelogic.utils

import java.util.concurrent.atomic.AtomicLong

import gamelogic.entities.Entity

final class EntityIdGenerator(startingId: Entity.Id) extends LongGenerator {
  protected val generator = new AtomicLong(startingId)
}
