package concurrent

import scala.reflect.ClassTag

class ActionBufferSpecs extends munit.FunSuite with BufferSpecs {

  def buffer[T](using ClassTag[T]): Buffer[T] = ActionBuffer()

}
