package gamelogic.utils

import java.util.concurrent.atomic.AtomicLong

trait LongGenerator[Id](fromLong: Long => Id) extends (() => Id) {

  protected val generator: AtomicLong

  inline def nextId(): Id = fromLong(generator.getAndIncrement())

  inline def apply(): Id = nextId()

  def currentValue: Id = fromLong(generator.get())

}
