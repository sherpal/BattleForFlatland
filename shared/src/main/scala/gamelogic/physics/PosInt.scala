package gamelogic.physics

import boopickle.Pickler
import io.circe.{Codec, Decoder, Encoder}

import scala.annotation.showAsInfix

opaque type PosInt = Int

object PosInt {

  extension (n: PosInt) {
    def +(that: PosInt): PosInt = n + that

    def minus(that: Int): Option[PosInt] = maybe(n - that)

    @showAsInfix
    def max(that: PosInt): PosInt = math.max(n, that)
    @showAsInfix
    def min(that: PosInt): PosInt = math.min(n, that)

    inline def toInt: Int = n

    inline def toLong: Long = toInt.toLong
  }

  def maybe(n: Int): Option[PosInt] = if n > 0 then Some(n) else None

  inline def of(inline n: Int): PosInt =
    inline if n <= 0 then compiletime.error(s"PosInt must be a positive integer, not $n")
    else n

  given Codec[PosInt]   = Codec.from(Decoder.decodeInt, Encoder.encodeInt)
  given Pickler[PosInt] = boopickle.Default.intPickler

  given Ordering[PosInt] = Ordering.Int

}
