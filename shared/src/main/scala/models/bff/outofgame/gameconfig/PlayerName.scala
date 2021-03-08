package models.bff.outofgame.gameconfig

import models.bff.outofgame.PlayerClasses

sealed trait PlayerName {
  def name: String
}

object PlayerName {

  case class HumanPlayerName(name: String) extends PlayerName
  case class AIPlayerName(cls: PlayerClasses, index: Int) extends PlayerName {
    def name: String = cls.toString ++ index.toString
  }

  import io.circe._
  import io.circe.generic.semiauto._
  implicit val fooDecoder: Decoder[PlayerName] = deriveDecoder[PlayerName]
  implicit val fooEncoder: Encoder[PlayerName] = deriveEncoder[PlayerName]

}
