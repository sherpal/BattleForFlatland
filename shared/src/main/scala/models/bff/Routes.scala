package models.bff

import urldsl.language.PathSegment.dummyErrorImpl._

object Routes {

  final val bff = root / "bff"

  final val allGames    = bff / "games"
  final val newMenuGame = bff / "new-game"

}
