package gamelogic.utils

import boopickle.Pickler
import io.circe.{Codec, Decoder, Encoder}
import models.syntax.Pointed

trait OpaqueDoubleCompanion[L >: Double <: Double] {

  extension (l: L) {
    inline def value: Double = l
  }

  inline def zero: L = 0.0
  inline def one: L  = 1.0

  inline def dummy: L = 1.0

  inline def fromDouble(l: Double): L = l

  given Codec[L] = Codec.from(Decoder.decodeDouble, Encoder.encodeDouble)

  given Pickler[L] = boopickle.Default.doublePickler

  given Ordering[L] = Ordering.fromLessThan(_ < _)

  given Pointed[L] = Pointed.factory(zero)

}
