package models.bff.outofgame

import io.circe.{Decoder, Encoder}

import scala.util.Try

/** Classes that a user can chose from. */
sealed trait PlayerClasses

object PlayerClasses {

  case object Square extends PlayerClasses
  case object Hexagon extends PlayerClasses

  final val allChoices: List[PlayerClasses] = List(Square, Hexagon)

  def playerClassByName(name: String): Option[PlayerClasses] = allChoices.find(_.toString == name)

  implicit val circeDecoder: Decoder[PlayerClasses] =
    Decoder.decodeString.emapTry(str => Try(playerClassByName(str).get))

  implicit val circeEncoder: Encoder[PlayerClasses] = Encoder.encodeString.contramap(_.toString)

}
