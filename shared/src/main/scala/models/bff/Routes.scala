package models.bff

import urldsl.language.PathSegment.dummyErrorImpl._
import urldsl.language.QueryParameters.dummyErrorImpl._

object Routes {

  final val bff = root / "bff"

  final val allGames    = bff / "games"
  final val newMenuGame = bff / "new-game"

  final val joinGame        = bff / "join-game"
  final val joinGameParam   = param[String]("gameId")
  final val gameJoined      = bff / "game-joined"
  final val gameJoinedParam = param[String]("gameId")

}
