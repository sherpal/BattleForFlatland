package gamelogic.utils

import java.util.concurrent.atomic.AtomicLong

import gamelogic.abilities.Ability

final class AbilityUseIdGenerator(startingId: Ability.UseId) extends LongGenerator {
  protected val generator: AtomicLong = new AtomicLong(startingId)
}
