package bffclient.game

/** How a game started from the menus was left. */
enum GameExit {

  /** The game ended. */
  case Ended

  /** The player left during the game (possibly the only human player). */
  case LeftEarly(onlyHuman: Boolean)

  /** The game server closed the connection before the end of the game. */
  case ConnectionLost
  case CannotConnect(error: String)
}
