package services.hostelections

import menus.data.{HostCandidacy, User}
import errors.ErrorADT

import java.util.concurrent.CountDownLatch

/** The elections of the players who run the game servers (see [[programs.launchGame]]).
  *
  * When a game is launched, an election is [[open]]ed with its human players. They send their
  * candidacy, then the election is closed (when everyone answered, or after a while), which ranks
  * the candidates (see [[HostCandidacy.ranking]]). The first one becomes the host. Should they fail
  * to run the game server, the next one becomes the host, and so on. The election is over when the
  * game server is ready.
  */
final class HostElections {
  import HostElections.Election

  println("Initializing HostElections service...")

  // by game id
  private var elections = Map.empty[Int, Election]

  /** Opens the election of the host of a game, among these players. The latch is released once they
    * all sent their candidacy.
    */
  def open(
      gameId: Int,
      gameSecret: String,
      gameCreator: String,
      players: Set[String]
  ): CountDownLatch = synchronized {
    val everyoneAnswered = CountDownLatch(1)
    elections += gameId -> Election(
      gameSecret,
      gameCreator,
      players,
      Map.empty,
      everyoneAnswered,
      None,
      None
    )
    everyoneAnswered
  }

  def candidacy(user: User, candidacy: HostCandidacy): Either[ErrorADT, Unit] = synchronized {
    elections.get(candidacy.gameId) match {
      case None => Left(ErrorADT.GameDoesNotExist(candidacy.gameId))
      case Some(election) if !election.players.contains(user.name) =>
        Left(ErrorADT.YouAreNotInGame(candidacy.gameId))
      case Some(election) =>
        val updated =
          election.copy(candidacies = election.candidacies + (user.name -> candidacy))
        elections += candidacy.gameId -> updated
        if updated.candidacies.keySet == updated.players then updated.everyoneAnswered.countDown()
        Right(())
    }
  }

  /** No more candidacies: ranks the candidates. */
  def close(gameId: Int): Unit = synchronized {
    elections = elections.updatedWith(gameId)(_.map { election =>
      election.copy(remaining =
        Some(HostCandidacy.ranking(election.candidacies, election.gameCreator))
      )
    })
  }

  /** The next candidate becomes the host (with the game secret, to give to them), if there is one
    * left.
    */
  def nextHost(gameId: Int): Option[(String, String)] = synchronized {
    elections.get(gameId).flatMap(election => election.remaining.map(election -> _)) match {
      case Some((election, next +: others)) =>
        elections += gameId -> election.copy(remaining = Some(others), host = Some(next))
        Some((next, election.gameSecret))
      case Some((election, _)) =>
        elections += gameId -> election.copy(host = None)
        None
      case None => None
    }
  }

  def isHost(gameId: Int, userName: String): Boolean = synchronized {
    elections.get(gameId).flatMap(_.host).contains(userName)
  }

  /** The election is over (the game server is ready, or the game could not start). Returns who
    * hosts it.
    */
  def over(gameId: Int): Option[String] = synchronized {
    val host = elections.get(gameId).flatMap(_.host)
    elections -= gameId
    host
  }
}

object HostElections {

  private case class Election(
      gameSecret: String,
      gameCreator: String,
      players: Set[String],
      candidacies: Map[String, HostCandidacy],
      everyoneAnswered: CountDownLatch,
      remaining: Option[Vector[String]], // once closed
      host: Option[String]
  )
}
