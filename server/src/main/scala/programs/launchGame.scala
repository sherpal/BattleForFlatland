package programs

import services.events.Event
import menus.data.{GameCredentialsWithGameInfo, User}
import errors.ErrorADT
import server.BackendEnv

import scala.concurrent.duration.*

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
    gameId: Int,
    rendezvousPort: Int
)(using env: BackendEnv): Either[ErrorADT, Unit] =
  env.menuGames.launchGame(requester, gameId).map { credentials =>
    env.events.dispatchEvent(Event.GameStarted(gameId))
    fork(s"host-election-$gameId")(electHost(credentials, rendezvousPort))
  }

/** Blocks until everyone sent their candidacy (or [[candidacyTimeout]]). */
private def electHost(
    credentials: GameCredentialsWithGameInfo,
    rendezvousPort: Int
)(using env: BackendEnv): Unit = {
  val game = credentials.gameInfo
  val everyoneAnswered = env.hostElections.open(
    game.id,
    credentials.secret,
    game.game.gameCreator.name,
    game.players.map(_.name).toSet
  )
  env.events.dispatchEvent(Event.HostCandidacyRequested(game.id))
  everyoneAnswered.await(candidacyTimeout.toMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
  env.hostElections.close(game.id)
  nextHost(game.id, rendezvousPort, lastFailure = None)
}

private def nextHost(
    gameId: Int,
    rendezvousPort: Int,
    lastFailure: Option[String]
)(using env: BackendEnv): Unit =
  env.hostElections.nextHost(gameId) match {
    case None =>
      launchFailed(
        gameId,
        lastFailure.fold(
          "No player can run the game server: they turned it off in their settings, or the menu " +
            "server has no Java runtime for their platform and their computer has no Java 17 or " +
            "later."
        )(failure => s"No player could run the game server ($failure).")
      )
    case Some((host, gameSecret)) =>
      println(s"$host hosts the game $gameId")
      env.events.dispatchEvent(Event.HostElected(gameId, host, gameSecret, rendezvousPort))
      fork(s"host-timeout-$gameId") {
        Thread.sleep(gameServerReadyTimeout.toMillis)
        hostFailed(gameId, host, s"the game server of $host was not ready in time", rendezvousPort)
      }
  }

/** The host could not run the game server: the next candidate takes over. Nothing happens if they
  * are not the host (anymore).
  */
def hostFailed(
    gameId: Int,
    host: String,
    reason: String,
    rendezvousPort: Int
)(using env: BackendEnv): Unit =
  if env.hostElections.isHost(gameId, host) then {
    println(s"Host $host of $gameId failed: $reason")
    nextHost(gameId, rendezvousPort, Some(reason))
  }

private def launchFailed(gameId: Int, reason: String)(using env: BackendEnv): Unit = {
  println(s"Game $gameId could not start: $reason")
  env.hostElections.over(gameId)
  env.menuGames.cancelLaunch(gameId)
  env.events.dispatchEvent(Event.GameLaunchFailed(gameId, reason))
  env.events.dispatchEvent(Event.GameDataRefreshed(Some(gameId)))
}
