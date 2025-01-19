package concurrent

trait Buffer[T] {
  def addActions(actions: Vector[T]): Unit

  def flush(): Vector[T]
}
