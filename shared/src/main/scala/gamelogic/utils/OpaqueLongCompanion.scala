package gamelogic.utils

import io.circe.Codec
import io.circe.Decoder
import io.circe.Encoder
import boopickle.Pickler
import models.syntax.Pointed

trait OpaqueLongCompanion[L >: Long <: Long] {

  extension (l: L) {
    inline def value: Long = l
  }

  inline def zero: L = 0L

  inline def dummy: L = 1L

  inline def fromLong(l: Long): L = l

  given Codec[L] = Codec.from(Decoder.decodeLong, Encoder.encodeLong)

  given Pickler[L] = boopickle.Default.longPickler

  given Ordering[L] = Ordering.fromLessThan(_ < _)

  given Pointed[L] = Pointed.factory(zero)

}
