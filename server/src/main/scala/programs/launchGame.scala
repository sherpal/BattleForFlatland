package programs

import zio.*
import services.*
import services.events.Event
import menus.data.{GameCredentialsWithGameInfo, User}
import errors.ErrorADT
import server.BackendEnv

/** How long the players have to send their candidacy to run the game server. */
private val candidacyTimeout = 5.seconds

/** How long the host has to start the game server (they may have to download it first). */
private val gameServerReadyTimeout = 3.minutes

/** Launches the game: the game server is run by one of its players, the "host".
  *
  *   - every player is asked for their candidacy (`HostCandidacyRequested`), which they send to
  *     `api/bff/host-candidacy`;
  *   - once they all answered (or after [[candidacyTimeout]]), the best candidate is told to run
  *     the game server (`HostElected`);
  *   - the game server tells the menu server that it is ready
  *     (`api/bff/internal/game-server-ready`), and everyone receives where to reach it.
  *
  * When the host cannot run the game server (they say so, see [[hostFailed]], or it is not ready in
  * time), the next candidate is chosen. Without any candidate left, the launch fails, and the game
  * can be launched again.
  */
def launchGame(
    requester: User,
    gameId: String,
    rendezvousPort: Int
): ZIO[BackendEnv, ErrorADT, Unit] = for {
  credentials <- menugames.launchGame(requester, gameId).flatMap(ZIO.fromEither)
  _           <- events.dispatchEvent(Event.GameStarted(gameId))
  _           <- electHost(credentials, rendezvousPort).forkDaemon
} yield ()

private def electHost(
    credentials: GameCredentialsWithGameInfo,
    rendezvousPort: Int
): ZIO[BackendEnv, Nothing, Unit] = {
  val game = credentials.gameInfo
  for {
    everyoneAnswered <- hostelections.open(
      game.id,
      credentials.secret,
      game.game.gameCreator.name,
      game.players.map(_.name).toSet
    )
    _ <- events.dispatchEvent(Event.HostCandidacyRequested(game.id))
    _ <- everyoneAnswered.await.timeout(candidacyTimeout)
    _ <- hostelections.close(game.id)
    _ <- nextHost(game.id, rendezvousPort, lastFailure = None)
  } yield ()
}

private def nextHost(
    gameId: String,
    rendezvousPort: Int,
    lastFailure: Option[String]
): ZIO[BackendEnv, Nothing, Unit] =
  hostelections.nextHost(gameId).flatMap {
    case None =>
      launchFailed(
        gameId,
        lastFailure.fold(
          "No player can run the game server: the menu server has no Java runtime for their " +
            "platform, and their computer has no Java 17 or later."
        )(failure => s"No player could run the game server ($failure).")
      )
    case Some((host, gameSecret)) =>
      for {
        _ <- Console.printLine(s"$host hosts the game $gameId").ignore
        _ <- events.dispatchEvent(Event.HostElected(gameId, host, gameSecret, rendezvousPort))
        _ <- (ZIO.sleep(gameServerReadyTimeout) *> hostFailed(
          gameId,
          host,
          s"the game server of $host was not ready in time",
          rendezvousPort
        )).forkDaemon
      } yield ()
  }

/** The host could not run the game server: the next candidate takes over. Nothing happens if they
  * are not the host (anymore).
  */
def hostFailed(
    gameId: String,
    host: String,
    reason: String,
    rendezvousPort: Int
): ZIO[BackendEnv, Nothing, Unit] =
  ZIO
    .whenZIO(hostelections.isHost(gameId, host))(
      Console.printLine(s"Host $host of $gameId failed: $reason").ignore *>
        nextHost(gameId, rendezvousPort, Some(reason))
    )
    .unit

private def launchFailed(gameId: String, reason: String): ZIO[BackendEnv, Nothing, Unit] = for {
  _ <- Console.printLine(s"Game $gameId could not start: $reason").ignore
  _ <- hostelections.over(gameId)
  _ <- menugames.cancelLaunch(gameId)
  _ <- events.dispatchEvent(Event.GameLaunchFailed(gameId, reason))
  _ <- events.dispatchEvent(Event.GameDataRefreshed(Some(gameId)))
} yield ()
