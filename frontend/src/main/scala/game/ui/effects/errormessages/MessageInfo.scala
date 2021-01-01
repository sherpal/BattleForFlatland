package game.ui.effects.errormessages

import scala.concurrent.duration._

private[errormessages] final case class MessageInfo(message: String, appearedAt: Long) {

  import MessageInfo._

  def alphaValue(currentTime: Long): Double = {
    val remainingTime = totalDuration.toMillis - (currentTime - appearedAt)
    val alphaCoef     = (fadeOutTime.toMillis - remainingTime).toDouble / fadeOutTime.toMillis
    if (alphaCoef < 0) 1.0
    else 1.0 - alphaCoef
  }

}

private[errormessages] object MessageInfo {
  val fadeOutTime: FiniteDuration   = 500.millis
  val totalDuration: FiniteDuration = 2.seconds
}
