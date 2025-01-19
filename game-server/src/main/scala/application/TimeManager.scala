package application

trait TimeManager {

  protected def sleep(millis: Long): Unit = {
    val startTime = System.currentTimeMillis()
    while {
      Thread.`yield`()
      System.currentTimeMillis() - startTime < millis
    } do ()
  }

}
