package models.bff.outofgame

import gamelogic.entities.classes.PlayerClassBuilder
import io.circe.{Decoder, Encoder}

import scala.util.Try

/** Classes that a user can chose from. */
sealed trait PlayerClasses {
  def builder: PlayerClassBuilder

  final def value = toString
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

  final val allChoices: List[PlayerClasses] = List(Triangle, Square, Pentagon, Hexagon)

  def playerClassByName(name: String): Option[PlayerClasses] = allChoices.find(_.value == name)

  given Decoder[PlayerClasses] =
    Decoder.decodeString.emapTry(str => Try(playerClassByName(str).get))

  given Encoder[PlayerClasses] = Encoder.encodeString.contramap(_.value)

}
