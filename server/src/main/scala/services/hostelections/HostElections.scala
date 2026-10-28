package services.hostelections

import zio.*
import menus.data.{HostCandidacy, User}
import errors.ErrorADT

/** The elections of the players who run the game servers (see [[programs.launchGame]]).
  *
  * When a game is launched, an election is [[open]]ed with its human players. They send their
  * candidacy, then the election is closed (when everyone answered, or after a while), which ranks
  * the candidates (see [[HostCandidacy.ranking]]). The first one becomes the host. Should they fail
  * to run the game server, the next one becomes the host, and so on. The election is over when the
  * game server is ready.
  */
trait HostElections {

  /** Opens the election of the host of a game, among these players. The promise completes once they
    * all sent their candidacy.
    */
  def open(
      gameId: String,
      gameSecret: String,
      gameCreator: String,
      players: Set[String]
  ): ZIO[Any, Nothing, Promise[Nothing, Unit]]

  def candidacy(user: User, candidacy: HostCandidacy): ZIO[Any, Nothing, Either[ErrorADT, Unit]]

  /** No more candidacies: ranks the candidates. */
  def close(gameId: String): ZIO[Any, Nothing, Unit]

  /** The next candidate becomes the host (with the game secret, to give to them), if there is one
    * left.
    */
  def nextHost(gameId: String): ZIO[Any, Nothing, Option[(String, String)]]

  def isHost(gameId: String, userName: String): ZIO[Any, Nothing, Boolean]

  /** The election is over (the game server is ready, or the game could not start). Returns who
    * hosts it.
    */
  def over(gameId: String): ZIO[Any, Nothing, Option[String]]
}

object HostElections {

  private case class Election(
      gameSecret: String,
      gameCreator: String,
      players: Set[String],
      candidacies: Map[String, HostCandidacy],
      everyoneAnswered: Promise[Nothing, Unit],
      remaining: Option[Vector[String]], // once closed
      host: Option[String]
  )

  val live: ZLayer[Any, Nothing, HostElections] = ZLayer.fromZIO(
    Ref.make(Map.empty[String, Election]).map { ref =>
      new HostElections {
        def open(gameId: String, gameSecret: String, gameCreator: String, players: Set[String]) =
          for {
            promise <- Promise.make[Nothing, Unit]
            _ <- ref.update(
              _ + (gameId -> Election(
                gameSecret,
                gameCreator,
                players,
                Map.empty,
                promise,
                None,
                None
              ))
            )
          } yield promise

        def candidacy(user: User, candidacy: HostCandidacy) = for {
          result <- ref.modify(elections =>
            elections.get(candidacy.gameId) match {
              case None => (Left(ErrorADT.GameDoesNotExist(candidacy.gameId)), elections)
              case Some(election) if !election.players.contains(user.name) =>
                (Left(ErrorADT.YouAreNotInGame(candidacy.gameId)), elections)
              case Some(election) =>
                val updated =
                  election.copy(candidacies = election.candidacies + (user.name -> candidacy))
                (Right(updated), elections + (candidacy.gameId -> updated))
            }
          )
          _ <- result match {
            case Right(election) if election.candidacies.keySet == election.players =>
              election.everyoneAnswered.succeed(())
            case _ => ZIO.unit
          }
        } yield result.map(_ => ())

        def close(gameId: String) = ref.update(_.updatedWith(gameId)(_.map { election =>
          election.copy(remaining =
            Some(HostCandidacy.ranking(election.candidacies, election.gameCreator))
          )
        }))

        def nextHost(gameId: String) = ref.modify(elections =>
          elections.get(gameId).flatMap(election => election.remaining.map(election -> _)) match {
            case Some((election, next +: others)) =>
              (
                Some((next, election.gameSecret)),
                elections + (gameId -> election.copy(remaining = Some(others), host = Some(next)))
              )
            case Some((election, _)) =>
              (None, elections + (gameId -> election.copy(host = None)))
            case None => (None, elections)
          }
        )

        def isHost(gameId: String, userName: String) =
          ref.get.map(_.get(gameId).flatMap(_.host).contains(userName))

        def over(gameId: String) =
          ref.modify(elections => (elections.get(gameId).flatMap(_.host), elections - gameId))
      }
    }
  )
}
