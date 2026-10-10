package gamelogic.utils

import boopickle.Pickler
import io.circe.{Codec, Decoder, Encoder}
import models.syntax.Pointed

trait OpaqueIntCompanion[L >: Int <: Int] {

  extension (l: L) {
    inline def value: Int = l
  }

  inline def zero: L = 0

  inline def dummy: L = 1

  inline def fromInt(l: Int): L = l

  given Codec[L] = Codec.from(Decoder.decodeInt, Encoder.encodeInt)

  given Pickler[L] = boopickle.Default.intPickler

  given Ordering[L] = Ordering.fromLessThan(_ < _)

  given Pointed[L] = Pointed.factory(zero)

}
