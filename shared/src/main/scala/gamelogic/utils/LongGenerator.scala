package gamelogic.utils

import java.util.concurrent.atomic.AtomicLong

trait LongGenerator extends (() => Long) {

  protected val generator: AtomicLong

  inline def nextId(): Long = generator.getAndIncrement()

  inline def apply(): Long = nextId()

  def currentValue: Long = generator.get()

}
