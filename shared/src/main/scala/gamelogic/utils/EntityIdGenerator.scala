package gamelogic.utils

import java.util.concurrent.atomic.AtomicLong

import gamelogic.entities.Entity

final class EntityIdGenerator(startingId: Entity.Id)
    extends LongGenerator[Entity.Id](Entity.Id.fromLong) {
  protected val generator = AtomicLong(startingId.value)
}
