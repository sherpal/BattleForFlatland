package bffclient.game

/** How a game started from the menus was left. */
enum GameExit {

  /** The game ended (its server can be stopped). */
  case Ended

  /** The player left during the game (if the only human player, its server can be stopped). */
  case LeftEarly(onlyHuman: Boolean)

  /** The game server closed the connection before the end of the game. */
  case ConnectionLost
  case CannotConnect(error: String)
}
