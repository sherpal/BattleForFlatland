package models.bff.outofgame

import gamelogic.entities.classes.PlayerClassBuilder
import io.circe.{Decoder, Encoder}

import scala.util.Try

/** Classes that a user can chose from. */
sealed trait PlayerClasses {
  def builder: PlayerClassBuilder
}

object PlayerClasses {

  case object Square extends PlayerClasses {
    def builder: PlayerClassBuilder = gamelogic.entities.classes.Square
  }
  case object Hexagon extends PlayerClasses {
    def builder: PlayerClassBuilder = gamelogic.entities.classes.Hexagon
  }
  case object Triangle extends PlayerClasses {
    def builder: PlayerClassBuilder = gamelogic.entities.classes.Triangle
  }
  case object Pentagon extends PlayerClasses {
    def builder: PlayerClassBuilder = gamelogic.entities.classes.Pentagon
  }

  final val allChoices: List[PlayerClasses] = List(Square, Hexagon, Triangle, Pentagon)

  def playerClassByName(name: String): Option[PlayerClasses] = allChoices.find(_.toString == name)

  implicit val circeDecoder: Decoder[PlayerClasses] =
    Decoder.decodeString.emapTry(str => Try(playerClassByName(str).get))

  implicit val circeEncoder: Encoder[PlayerClasses] = Encoder.encodeString.contramap(_.toString)

}
