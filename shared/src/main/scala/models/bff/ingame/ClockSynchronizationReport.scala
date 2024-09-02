package models.bff.ingame

import models.bff.ingame.ClockSynchronizationReport.*

final case class ClockSynchronizationReport(results: Vector[PingPongResult]) {
  def add(result: PingPongResult): ClockSynchronizationReport =
    copy(results = results :+ result)

  private lazy val deltas = results.collect { case successResult: PingPongSuccess =>
    successResult.delta
  }

  def delta: Double = deltas.sum.toDouble / deltas.length

  def deltaAsLong = delta.toLong

  def printableString = s"""Clock Sync Report:
                           |Total sample: ${results.length}
                           |Successes:    ${deltas.length}
                           |Lost:         ${results.length - deltas.length}
                           |Delta:        $delta
                           |Delta range:  ${deltas.min} - ${deltas.max}
                           |""".stripMargin
}

object ClockSynchronizationReport {

  def empty = ClockSynchronizationReport(Vector.empty)

  sealed trait PingPongResult {
    def startingTime: Long
  }

  case class PingPongSuccess(startingTime: Long, midwayTime: Long, endTime: Long)
      extends PingPongResult {
    def latency  = (endTime - startingTime) / 2
    def linkTime = latency + midwayTime
    def delta    = linkTime - endTime
  }

  case class PingPongFailure(startingTime: Long, failure: String) extends PingPongResult

}
