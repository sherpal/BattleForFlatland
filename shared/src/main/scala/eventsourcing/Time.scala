package eventsourcing

import io.circe.{Decoder, Encoder}

opaque type Time = Long

object Time {

  def now(): Time       = System.currentTimeMillis() / 1000 // second precision as in sqlite
  inline def zero: Time = 0L

  given Ordering[Time] = Ordering.fromLessThan(_ < _)

  given Conversion[Time, Ordered[Time]] = Ordered.orderingToOrdered(_)

  given Encoder[Time] = Encoder.encodeLong
  given Decoder[Time] = Decoder.decodeLong

  private[eventsourcing] inline def fromValue(value: Long): Time = value

  extension (time: Time) {
    inline def value: Long = time

    def +(timey: Long): Time = time + timey
  }

}
