package gamelogic.gameextras

import io.circe.Encoder

/**
  * A [[GameMarker]] is a token that players can put in the game in order to improve
  * communication via some "markers".
  *
  * There are a fixed, hardcoded number of markers that player can use, and they are all
  * implemented below. Markers have no game semantic and, even though we encode them in the
  * [[gamelogic.gamestate.GameState]] for simplicity, IAs will never use them.
  *
  * Markers can be put either in a fixed position in the game, or they can be assigned
  * to a particular entity (friend or foe).
  */
sealed trait GameMarker {
  def id: Int
}

object GameMarker {

  case object Cross extends GameMarker {
    def id: Int = 1
  }
  case object Lozenge extends GameMarker {
    def id: Int = 2
  }
  case object Moon extends GameMarker {
    def id: Int = 3
  }
  case object Square extends GameMarker {
    def id: Int = 4
  }
  case object Star extends GameMarker {
    def id: Int = 5
  }
  case object Triangle extends GameMarker {
    def id: Int = 6
  }

  val allMarkers = List(Cross, Lozenge, Moon, Square, Star, Triangle)

  implicit val jsonEncoder: Encoder[GameMarker] =
    Encoder.encodeInt.contramap(_.id)

}
