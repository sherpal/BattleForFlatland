package gamelogic.gameextras

// todo[scala3] change to enum
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
sealed trait GameMarker

object GameMarker {

  case object Cross extends GameMarker
  case object Lozenge extends GameMarker
  case object Moon extends GameMarker
  case object Square extends GameMarker
  case object Star extends GameMarker
  case object Triangle extends GameMarker

  val allMarkers: List[GameMarker] = List(Cross, Lozenge, Moon, Square, Star, Triangle)

}
