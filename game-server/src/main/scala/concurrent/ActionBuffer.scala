package concurrent

import java.util.concurrent.atomic.AtomicReference
import scala.reflect.ClassTag
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ActionBuffer[T] extends Buffer[T] {
  private val buffer = AtomicReference(Vector.empty[T])

  def addActions(actions: Vector[T]): Unit =
    if actions.nonEmpty then buffer.getAndUpdate(_ ++ actions)

  def flush(): Vector[T] = buffer.getAndUpdate(_ => Vector.empty[T])
}
