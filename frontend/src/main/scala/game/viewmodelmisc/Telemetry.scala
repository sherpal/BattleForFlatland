package game.viewmodelmisc

import scala.scalajs.js

final case class Telemetry(
    lastFrameTicks: js.Array[Long]
) {

  def addFPSDataPoint(millis: Long): Telemetry =
    copy(lastFrameTicks =
      lastFrameTicks.slice(if lastFrameTicks.length >= 100 then 1 else 0, 100).append(millis)
    )

  def fps: Telemetry.FPSReport = if lastFrameTicks.isEmpty then Telemetry.FPSReport(0, 0, 0)
  else
    Telemetry.FPSReport(
      1000 / (lastFrameTicks.sum / lastFrameTicks.length),
      lastFrameTicks.max,
      lastFrameTicks.min
    )

}

object Telemetry {
  def empty: Telemetry = Telemetry(js.Array())

  case class FPSReport(
      average: Long,
      max: Long,
      min: Long
  )
}
