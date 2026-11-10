package server

import models.bff.outofgame.MenuGameWithPlayers
import menus.data.APIResponse
import menus.data.JoinGameFormData
import errors.ErrorADT
import menus.data.User
import menus.data.CreateGameFormData
import menus.data.ChangePlayerInfoFormData
import services.events.Event
import menus.data.*

//noinspection TypeAnnotation
class MenuGameRoutes(rendezvousPort: Int, lobby: LobbyServer)(using env: BackendEnv)
    extends cask.Routes
    with JsonEndpoints {

  import env.{events, hostElections, menuGames}

  programs.userConnectedWatcher

  private def gameDataRefreshed(gameId: String): Unit =
    events.dispatchEvent(Event.GameDataRefreshed(Some(gameId)))

  @getJ[Vector[MenuGameWithPlayers]]("api/bff/games")
  def games() = menuGames.menuGames.map(_.forgetPassword)

  @loggedIn()
  @readBody[GameIdFormData]
  @postJ[APIResponse[Boolean]]("api/bff/start-game")
  def launchGame()(body: GameIdFormData)(user: User) =
    APIResponse.fromEither(programs.launchGame(user, body.gameId, rendezvousPort).map(_ => true))

  @loggedIn()
  @readBody[HostCandidacy]
  @postJ[APIResponse[Boolean]]("api/bff/host-candidacy")
  def hostCandidacy()(body: HostCandidacy)(user: User) =
    APIResponse.fromEither(hostElections.candidacy(user, body).map(_ => true))

  @loggedIn()
  @readBody[HostFailedFormData]
  @postJ[APIResponse[Boolean]]("api/bff/host-failed")
  def hostFailed()(body: HostFailedFormData)(user: User) = {
    programs.hostFailed(body.gameId, user.name, s"${user.name}: ${body.reason}", rendezvousPort)
    APIResponse.fromEither(Right(true))
  }

  @loggedIn()
  @readBody[JoinGameFormData]
  @postJ[APIResponse[Vector[MenuGameWithPlayers]]]("api/bff/join-game")
  def joinGame()(body: JoinGameFormData)(user: User) = APIResponse.fromEither(
    menuGames.joinGame(user, body.gameId, body.maybePassword).map { games =>
      gameDataRefreshed(body.gameId)
      games
    }
  )

  @loggedIn()
  @readBody[GameIdFormData]
  @postJ[APIResponse[MenuGameWithPlayers]]("api/bff/leave-game")
  def leaveGame()(body: GameIdFormData)(user: User) = APIResponse.fromEither(
    menuGames.removePlayer(user, body.gameId).map { game =>
      if game.game.gameCreator == user then menuGames.deleteGame(body.gameId)
      gameDataRefreshed(body.gameId)
      game
    }
  )

  @loggedIn()
  @readBody[KickPlayerFormData]
  @postJ[APIResponse[MenuGameWithPlayers]]("api/bff/kick-player")
  def kickPlayer()(body: KickPlayerFormData)(user: User) = APIResponse.fromEither(for {
    game <- menuGames.menuGames
      .find(_.id == body.gameId)
      .toRight(ErrorADT.GameDoesNotExist(body.gameId))
    _ <- Either.cond(game.game.gameCreator == user, (), ErrorADT.YouAreNotCreator(user.name))
    updatedGame <- menuGames.removePlayer(User(body.playerName), body.gameId)
  } yield {
    gameDataRefreshed(body.gameId)
    updatedGame
  })

  @loggedIn()
  @readBody[CreateGameFormData]
  @postJ[APIResponse[MenuGameWithPlayers]]("api/bff/new-game")
  def createGame()(body: CreateGameFormData)(user: User) = APIResponse.fromEither(
    menuGames.createGame(body.gameName, None, user).map { game => // no password for now
      gameDataRefreshed(game.id)
      game
    }
  )

  @loggedIn()
  @readBody[ChangePlayerInfoFormData]
  @postJ[APIResponse[MenuGameWithPlayers]]("api/bff/change-player-info")
  def changePlayerInfo()(body: ChangePlayerInfoFormData)(user: User) = APIResponse.fromEither(
    menuGames.changePlayerInfo(user, body.gameId, body.playerInfo).map { updatedGame =>
      gameDataRefreshed(body.gameId)
      updatedGame
    }
  )

  @loggedIn()
  @readBody[ChangeGameMetadataFormData]
  @postJ[APIResponse[MenuGameWithPlayers]]("api/bff/change-game-config")
  def changeGameConfiguration()(body: ChangeGameMetadataFormData)(user: User) =
    APIResponse.fromEither(
      menuGames.changeGameMetadata(user, body.gameId, body.gameMetadata).map { updatedGame =>
        gameDataRefreshed(body.gameId)
        updatedGame
      }
    )

  @loggedIn()
  @readBody[GameIdFormData]
  @postJ[APIResponse[Boolean]]("api/bff/add-ai-to-game")
  def addAIToGame()(body: GameIdFormData)(user: User) = APIResponse.fromEither(
    menuGames.addAIToGame(body.gameId).map { _ =>
      gameDataRefreshed(body.gameId)
      true
    }
  )

  @loggedIn()
  @readBody[GameIdFormData]
  @postJ[APIResponse[Boolean]]("api/bff/remove-ai-from-game")
  def removeAIFromGame()(body: RemoveAIFromGame)(user: User) = APIResponse.fromEither(
    menuGames.removeAIFromGame(body.gameId, body.cls).map { _ =>
      gameDataRefreshed(body.gameId)
      true
    }
  )

  /** Subscribes to the messages of the menu server (see `communication.udp.Lobby`): of the list of
    * games, or of the lobby of `gameId`.
    */
  @loggedIn()
  @readBody[LobbySubscribeFormData]
  @postJ[APIResponse[LobbySubscription]]("api/bff/lobby/subscribe")
  def subscribe()(body: LobbySubscribeFormData)(user: User) =
    APIResponse.fromEither(Right(lobby.subscribe(user, body.gameId)))

  /** The messages of the subscription after the `after`-th (which the client got already). */
  @loggedIn()
  @getJ[APIResponse[Vector[LobbyMessage]]]("api/bff/lobby/messages")
  def lobbyMessages(id: String, after: Long)(user: User) =
    APIResponse.fromEither(lobby.messages(user, id, after).toRight(ErrorADT.RawNotFound()))

  @loggedIn()
  @readBody[LobbyUnsubscribeFormData]
  @postJ[APIResponse[Boolean]]("api/bff/lobby/unsubscribe")
  def unsubscribe()(body: LobbyUnsubscribeFormData)(user: User) =
    APIResponse.fromEither(Right { lobby.unsubscribe(user, body.id); true })

  initialize()
}
