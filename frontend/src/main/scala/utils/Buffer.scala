package utils

import scala.scalajs.js

class Buffer[T](size: Int) {
  private var cursor = 0
  private val buffer = new js.Array[T](size)

  def addElem(t: T): Unit = {
    buffer(cursor) = t
    cursor += 1
  }

  def flush(): js.Array[T] = {
    val result = buffer.slice(0, cursor)
    cursor = 0
    result
  }
}
