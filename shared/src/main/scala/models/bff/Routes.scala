package models.bff

import urldsl.language.PathSegment.dummyErrorImpl.*
import urldsl.language.QueryParameters.dummyErrorImpl.*

object Routes {

  final val bff = root / "bff"

  final val allGames    = bff / "games"    // get
  final val newMenuGame = bff / "new-game" // post

  final val joinGame         = bff / "join-game"          // post
  final val joinGameParam    = param[String]("gameId")
  final val gameJoined       = bff / "game-joined"
  final val amIPlaying       = bff / "am-i-playing"       // get
  final val gameInfo         = bff / "game-info"          // get
  final val gameJoinedWS     = bff / "game-anti-chamber"  // web socket
  final val cancelGame       = bff / "cancel-game"        // post
  final val startGame        = bff / "start-game"         // post
  final val iAmStilThere     = bff / "i-am-still-there"   // post
  final val leaveGame        = bff / "leave-game"         // post
  final val kickPlayer       = bff / "kick-player"        // post
  final val changePlayerInfo = bff / "change-player-info" // post
  final val changeGameConfig = bff / "change-game-config" // post

  final val gamePlayingRoot = bff / "game-playing"
  final val inGame          = gamePlayingRoot / "in-game" // route (get)

  final val preFlightGameServer = root / "pre-flight"              // options
  final val gameServerToken     = root / "fetch-game-server-token" // post
  final val joinGameServer      = root / "connect"                 // ws
  final val inGameCancel        = inGame / "cancel-game"           // post
  final val inGameSettings      = root / "game-settings"           // route (get)

  final val gameIdParam          = param[String]("gameId")
  final val tokenParam           = param[String]("token")
  final val userIdAndTokenParams = param[String]("userId") & tokenParam

}
