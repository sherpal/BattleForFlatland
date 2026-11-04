package server

import services.menugames
import ziocask.WithZIOEndpoints
import models.bff.outofgame.MenuGameWithPlayers
import menus.data.APIResponse
import io.circe.Decoder
import menus.data.JoinGameFormData
import zio.*
import errors.ErrorADT
import menus.data.User
import menus.data.CreateGameFormData
import menus.data.ChangePlayerInfoFormData
import io.circe.Encoder
import services.events
import services.hostelections
import menus.data.*

//noinspection TypeAnnotation
class MenuGameRoutes(rendezvousPort: Int, lobby: LobbyServer)(using
    val runtime: zio.Runtime[BackendEnv]
) extends cask.Routes
    with WithZIOEndpoints[BackendEnv] {

  Unsafe.unsafe(implicit unsafe => runtime.unsafe.runOrFork(programs.userConnectedWatcher)) match {
    case Left(_) =>
    case Right(Exit.Success(_)) => throw IllegalStateException(s"Connection check fiber exitted")
    case Right(Exit.Failure(cause)) =>
      throw IllegalStateException("Connection check exitted with failure", cause.squashTrace)
  }

  @caskz.getJ[Vector[MenuGameWithPlayers]]("api/bff/games")
  def games() = menugames.menuGames.map(_.map(_.forgetPassword))

  @loggedIn()
  @readBody[GameIdFormData]
  @caskz.postJ[APIResponse[Boolean]]("api/bff/start-game")
  def launchGame()(body: GameIdFormData)(user: User) =
    programs
      .launchGame(user, body.gameId, rendezvousPort)
      .as(true)
      .either
      .map(APIResponse.fromEither)

  @loggedIn()
  @readBody[HostCandidacy]
  @caskz.postJ[APIResponse[Boolean]]("api/bff/host-candidacy")
  def hostCandidacy()(body: HostCandidacy)(user: User) =
    hostelections.candidacy(user, body).map(result => APIResponse.fromEither(result.map(_ => true)))

  @loggedIn()
  @readBody[HostFailedFormData]
  @caskz.postJ[APIResponse[Boolean]]("api/bff/host-failed")
  def hostFailed()(body: HostFailedFormData)(user: User) =
    programs
      .hostFailed(body.gameId, user.name, s"${user.name}: ${body.reason}", rendezvousPort)
      .as(APIResponse.fromEither(Right(true)))

  @loggedIn()
  @readBody[JoinGameFormData]
  @caskz.postJ[APIResponse[Vector[MenuGameWithPlayers]]]("api/bff/join-game")
  def joinGame()(body: JoinGameFormData)(user: User) = (for {
    gamesOrValFailure <- menugames.joinGame(user, body.gameId, body.maybePassword)
    games             <- ZIO.fromEither(gamesOrValFailure)
    _                 <- events.dispatchEvent(events.Event.GameDataRefreshed(Some(body.gameId)))
  } yield games).either.map(APIResponse.fromEither)

  @loggedIn()
  @readBody[GameIdFormData]
  @caskz.postJ[APIResponse[MenuGameWithPlayers]]("api/bff/leave-game")
  def leaveGame()(body: GameIdFormData)(user: User) = (for {
    maybeGame <- menugames.removePlayer(user, body.gameId)
    game      <- ZIO.fromEither(maybeGame)
    _         <- ZIO.when(game.game.gameCreator == user)(menugames.deleteGame(body.gameId))
    _         <- events.dispatchEvent(events.Event.GameDataRefreshed(Some(body.gameId)))
  } yield game).either.map(APIResponse.fromEither)

  @loggedIn()
  @readBody[KickPlayerFormData]
  @caskz.postJ[APIResponse[MenuGameWithPlayers]]("api/bff/kick-player")
  def kickPlayer()(body: KickPlayerFormData)(user: User) = (for {
    games <- menugames.menuGames
    game <- games
      .find(_.id == body.gameId)
      .map(ZIO.succeed(_))
      .getOrElse(ZIO.fail(ErrorADT.GameDoesNotExist(body.gameId)))
    _ <- ZIO.when(game.game.gameCreator != user)(ZIO.fail(ErrorADT.YouAreNotCreator(user.name)))
    result      <- menugames.removePlayer(User(body.playerName), body.gameId)
    updatedGame <- ZIO.fromEither(result)
    _           <- events.dispatchEvent(events.Event.GameDataRefreshed(Some(body.gameId)))
  } yield updatedGame).either.map(APIResponse.fromEither)

  @loggedIn()
  @readBody[CreateGameFormData]
  @caskz.postJ[APIResponse[MenuGameWithPlayers]]("api/bff/new-game")
  def createGame()(body: CreateGameFormData)(user: User) = (for {
    maybeNewGame <- menugames.createGame(body.gameName, None, user) // no password for now
    _ <- maybeNewGame match {
      case Left(_) => ZIO.unit
      case Right(game) => events.dispatchEvent(events.Event.GameDataRefreshed(Some(game.id)))
    }
  } yield maybeNewGame).map(APIResponse.fromEither)

  @loggedIn()
  @readBody[ChangePlayerInfoFormData]
  @caskz.postJ[APIResponse[MenuGameWithPlayers]]("api/bff/change-player-info")
  def changePlayerInfo()(body: ChangePlayerInfoFormData)(user: User) = (for {
    updatedGame <- menugames.changePlayerInfo(user, body.gameId, body.playerInfo)
    _ <- ZIO.when(updatedGame.isRight)(
      events.dispatchEvent(events.Event.GameDataRefreshed(Some(body.gameId)))
    )
  } yield updatedGame).map(APIResponse.fromEither)

  @loggedIn()
  @readBody[ChangeGameMetadataFormData]
  @caskz.postJ[APIResponse[MenuGameWithPlayers]]("api/bff/change-game-config")
  def changeGameConfiguration()(body: ChangeGameMetadataFormData)(user: User) = (for {
    updatedGame <- menugames.changeGameMetadata(user, body.gameId, body.gameMetadata)
    _ <- ZIO.when(updatedGame.isRight)(
      events.dispatchEvent(events.Event.GameDataRefreshed(Some(body.gameId)))
    )
  } yield updatedGame).map(APIResponse.fromEither)

  @loggedIn()
  @readBody[GameIdFormData]
  @caskz.postJ[APIResponse[Boolean]]("api/bff/add-ai-to-game")
  def addAIToGame()(body: GameIdFormData)(user: User) = (for {
    result <- menugames.addAIToGame(body.gameId)
    _ <- ZIO.when(result.isRight)(
      events.dispatchEvent(events.Event.GameDataRefreshed(Some(body.gameId)))
    )
  } yield result.map(_ => true)).map(APIResponse.fromEither)

  @loggedIn()
  @readBody[GameIdFormData]
  @caskz.postJ[APIResponse[Boolean]]("api/bff/remove-ai-from-game")
  def removeAIFromGame()(body: RemoveAIFromGame)(user: User) = (for {
    result <- menugames.removeAIFromGame(body.gameId, body.cls)
    _ <- ZIO.when(result.isRight)(
      events.dispatchEvent(events.Event.GameDataRefreshed(Some(body.gameId)))
    )
  } yield result.map(_ => true)).map(APIResponse.fromEither)

  /** Subscribes to the messages of the menu server (see `communication.udp.Lobby`): of the list of
    * games, or of the lobby of `gameId`.
    */
  @loggedIn()
  @readBody[LobbySubscribeFormData]
  @caskz.postJ[APIResponse[LobbySubscription]]("api/bff/lobby/subscribe")
  def subscribe()(body: LobbySubscribeFormData)(user: User) =
    ZIO.succeed(APIResponse.fromEither(Right(lobby.subscribe(user, body.gameId))))

  /** The messages of the subscription after the `after`-th (which the client got already). */
  @loggedIn()
  @caskz.getJ[APIResponse[Vector[LobbyMessage]]]("api/bff/lobby/messages")
  def lobbyMessages(id: String, after: Long)(user: User) =
    ZIO.succeed(
      APIResponse.fromEither(lobby.messages(user, id, after).toRight(ErrorADT.RawNotFound()))
    )

  @loggedIn()
  @readBody[LobbyUnsubscribeFormData]
  @caskz.postJ[APIResponse[Boolean]]("api/bff/lobby/unsubscribe")
  def unsubscribe()(body: LobbyUnsubscribeFormData)(user: User) =
    ZIO.succeed(APIResponse.fromEither(Right { lobby.unsubscribe(user, body.id); true }))

  initialize()
}
