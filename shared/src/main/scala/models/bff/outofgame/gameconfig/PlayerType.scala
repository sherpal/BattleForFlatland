package models.bff.outofgame.gameconfig

import models.syntax.Pointed

/**
  * Determines what kind of player is set in the config.
  * The three possibilities are Human (when the player will be controlled by a human),
  * ArtificialIntelligence (when the player will be handled by the server) or
  * Observer (when a human wants to look at the game but not play.)
  */
sealed trait PlayerType {
  def playing: Boolean = this match {
    case PlayerType.Observer => false
    case _                   => true
  }
}

object PlayerType {

  case object Human extends PlayerType
  case object ArtificialIntelligence extends PlayerType
  case object Observer extends PlayerType

  def playerTypeByName(playerTypeName: String): Option[PlayerType] =
    List(
      Human,
      ArtificialIntelligence,
      Observer
    ).find(_.toString == playerTypeName)

  def unsafePlayerTypeByName(playerTypeName: String): PlayerType = playerTypeByName(playerTypeName).get

  import io.circe._
  import io.circe.generic.semiauto._
  implicit val fooDecoder: Decoder[PlayerType] = deriveDecoder[PlayerType]
  implicit val fooEncoder: Encoder[PlayerType] = deriveEncoder[PlayerType]

  implicit val pointed: Pointed[PlayerType] = Pointed.factory(Human)

}
