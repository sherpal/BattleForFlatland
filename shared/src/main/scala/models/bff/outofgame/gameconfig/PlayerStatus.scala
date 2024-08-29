package models.bff.outofgame.gameconfig

import io.circe.{Decoder, Encoder}
import models.syntax.Pointed

import scala.util.Try

sealed trait PlayerStatus

object PlayerStatus {
  case object Ready    extends PlayerStatus
  case object NotReady extends PlayerStatus

  implicit def pointed: Pointed[PlayerStatus] =
    Pointed.factory(NotReady) // ensuring correct default

  final val statuses = List(Ready, NotReady)

  def fromBoolean(bool: Boolean): PlayerStatus = if (bool) Ready else NotReady

  given Decoder[PlayerStatus] =
    Decoder.decodeString.emapTry(str => Try(statuses.find(_.toString == str).get))
  given Encoder[PlayerStatus] = Encoder.encodeString.contramap(_.toString)
}
