package gamelogic.utils

import java.util.concurrent.atomic.AtomicLong

import gamelogic.abilities.Ability

final class AbilityUseIdGenerator(startingId: Ability.UseId)
    extends LongGenerator[Ability.UseId](Ability.UseId.fromLong) {
  protected val generator: AtomicLong = AtomicLong(startingId.value)
}
