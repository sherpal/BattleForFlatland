package bffclient.game

/** Credentials to connect to a game server, received from the menu server. */
final case class GameAccess(
    host: String,
    port: Int,
    userName: String,
    gameId: String,
    secret: String
)
