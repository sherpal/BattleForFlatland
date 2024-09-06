package models.bff.ingame

import io.circe.Codec

final case class ServerPerformanceSummary(
    averageFPS: Int,
    medianFPS: Int,
    maxTimeBetweenLoops: Long,
    usedMemory: Long
)

object ServerPerformanceSummary {
  given Codec[ServerPerformanceSummary] = io.circe.generic.semiauto.deriveCodec
}
