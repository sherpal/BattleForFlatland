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
import menus.data.MenuGameComm
import services.events
import services.hostelections
import java.util.concurrent.atomic.AtomicBoolean
import menus.data.*

//noinspection TypeAnnotation
class MenuGameRoutes(rendezvousPort: Int)(using
    val runtime: zio.Runtime[BackendEnv],
    ac: castor.Context,
    log: cask.util.Logger
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

  @cask.websocket("/ws/bff/game-anti-chamber")
  def gameAntiChamberWS(ctx: cask.Request, gameId: Option[String] = None): cask.WebsocketResult =
    maybeLoggedInUser(ctx) match {
      case None => unauthenticatedResponse
      case Some(user) =>
        cask.WsHandler { channel =>
          val isOpen    = AtomicBoolean(true)
          val isOpenZIO = ZIO.succeed(isOpen.get())

          def sendMsg(msg: MenuGameComm) = ZIO.succeed(
            channel.send(
              cask.Ws.Text(
                Encoder[MenuGameComm].apply(msg).noSpaces
              )
            )
          )

          // whether the event is about our game
          def ours(eventGameId: String) = gameId.contains[String](eventGameId)

          val registerEvents = events
            .registerEvents[
              events.Event.GameDataRefreshed | events.Event.GameStarted |
                events.Event.HostCandidacyRequested | events.Event.HostElected |
                events.Event.GameLaunchFailed | events.Event.GameCredentials
            ](isOpenZIO) {
              case events.Event.GameDataRefreshed(maybeGameId)
                  // we are interested in this event if
                  // - gameId is None, because that means we are in the list-games ui
                  // - maybeGameId is None, because then it could be any game, including the one we care about
                  // - maybeGameId is the exact gameId I care about
                  if gameId.forall(id => maybeGameId.forall(_ == id)) =>
                ZIO.whenZIO(isOpenZIO)(sendMsg(MenuGameComm.DataUpdated())).unit
              case events.Event.GameStarted(startedGameId) if ours(startedGameId) =>
                sendMsg(MenuGameComm.GameStarted())
              case events.Event.HostCandidacyRequested(electionGameId) if ours(electionGameId) =>
                sendMsg(MenuGameComm.HostCandidacyRequested())
              case events.Event.HostElected(electedGameId, host, gameSecret, rendezvousPort)
                  if ours(electedGameId) =>
                sendMsg(MenuGameComm.HostElected(host)) *> ZIO
                  .when(host == user.name)(
                    sendMsg(MenuGameComm.SpawnGameServer(electedGameId, gameSecret, rendezvousPort))
                  )
                  .unit
              case events.Event.GameLaunchFailed(failedGameId, reason) if ours(failedGameId) =>
                sendMsg(MenuGameComm.GameLaunchFailed(reason))
              case events.Event.GameCredentials(creds, addresses, host, port, rendezvousPort)
                  if ours(creds.gameCredentials.gameId) =>
                creds.allGameUserCredentials.find(_.userName == user.name) match {
                  case Some(userCreds) =>
                    // the host runs the game server: no need to go through the network
                    val loopback = Option
                      .when(host.contains[String](user.name))(GameServerAddress("127.0.0.1", port))
                    sendMsg(
                      MenuGameComm.HereAreYourCredentials(
                        userCreds.gameId,
                        userCreds.userSecret,
                        (loopback.toVector ++ addresses).distinct,
                        rendezvousPort
                      )
                    )
                  case None =>
                    ZIO.die(
                      IllegalStateException(
                        s"I received all game credentials but it did not contain the info ${user.name}."
                      )
                    )
                }
              case _ => ZIO.unit // not interested
            }
          val dispatchUserConnected = gameId
            .map(id => events.dispatchEvent(events.Event.UserConnectedSocket(user, id)))
            .getOrElse(ZIO.unit)
          runEffect(registerEvents *> dispatchUserConnected)
          cask.WsActor {
            case cask.Ws.Text(_) =>
              println(s"Received some text, going to ignore...")
            case _: cask.Ws.Close =>
              isOpen.set(false)
              gameId.foreach(id =>
                runEffect(events.dispatchEvent(events.Event.UserSocketDisconnected(user, id)))
              )

          }
        }

    }

  private def runEffect[R >: BackendEnv, E <: Throwable, A](effect: ZIO[R, E, A]) =
    Unsafe.unsafe(implicit unsafe => runtime.unsafe.run(effect).getOrThrow())

  initialize()
}
