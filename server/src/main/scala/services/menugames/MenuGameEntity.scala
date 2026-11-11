package services.menugames

import errors.ErrorADT
import eventsourcing.{Effect, EntityInformation}
import gamelogic.docs.BossMetadata
import io.circe.Codec
import menus.data.{AllGameCredentials, GameCredentials, GameCredentialsWithGameInfo, User}
import models.bff.ingame.GameUserCredentials
import models.bff.outofgame.gameconfig.*
import models.bff.outofgame.gameconfig.GameConfiguration.GameConfigMetadata
import models.bff.outofgame.{MenuGameWithPlayers, PlayerClasses}
import models.syntax.Pointed
import services.crypto.{Crypto, HashedPassword}
import utils.misc.RGBColour

/** A game of the menus, from its creation until it ends. Its id is given by [[menus.data.GameCreatedInfo]].
  *
  * The checks that involve the other games (unique names, players in only one game) are made by [[MenuGames]].
  */
enum MenuGameEntity {
  case NotCreated

  /** `credentials`: since it was last launched. */
  case Open(game: MenuGameWithPlayers, credentials: Option[GameCredentialsWithGameInfo])

  /** The game server of a game can still be running (and need its credentials) after it ended in the menus. */
  case Over(credentials: Option[GameCredentialsWithGameInfo])

  def maybeGame: Option[MenuGameWithPlayers] = this match {
    case Open(game, _) => Some(game)
    case _             => None
  }

  def maybeCredentials: Option[GameCredentialsWithGameInfo] = this match {
    case NotCreated              => None
    case Open(_, credentials)    => credentials
    case Over(credentials)       => credentials
  }
}

object MenuGameEntity {

  /** Every command replies the state of the game after it, or why it was refused. */
  type Reply = castor.Actor[Either[ErrorADT, MenuGameEntity]]

  enum Command:
    case Create(game: MenuGameWithPlayers, replyTo: Reply)
    case Join(user: User, maybePassword: Option[String], replyTo: Reply)
    case RemovePlayer(playerName: String, replyTo: Reply)
    case ChangePlayerInfo(user: User, playerInfo: PlayerInfo, replyTo: Reply)
    case ChangeMetadata(requester: User, metadata: GameConfigMetadata, replyTo: Reply)
    case AddAI(replyTo: Reply)
    case RemoveAI(cls: PlayerClasses, replyTo: Reply)
    case Launch(requester: User, replyTo: Reply)
    case CancelLaunch(replyTo: Reply)
    case End(replyTo: Reply)
    case Get(replyTo: Reply)

    def replyTo: Reply

    def handle(state: MenuGameEntity, id: Int): Effect[Event, MenuGameEntity] =
      decide(state, id) match {
        case Left(error)        => Effect.Ignore[Event, MenuGameEntity]().thenReply(replyTo)(_ => Left(error))
        case Right(None)        => Effect.Ignore[Event, MenuGameEntity]().thenReply(replyTo)(Right(_))
        case Right(Some(event)) => Effect.Persist[Event, MenuGameEntity](event).thenReply(replyTo)(Right(_))
      }

    /** The event to persist, if any. */
    private def decide(state: MenuGameEntity, id: Int): Either[ErrorADT, Option[Event]] = (this, state) match {
      case (Get(_), _)                   => Right(None)
      case (Create(game, _), NotCreated) => Right(Some(Event.GameCreated(game)))
      case (Create(_, _), _)             => Left(ErrorADT.RawInternalError(s"Game $id was already created"))
      case (RemoveAI(_, _), Open(game, _)) =>
        // todo: remove an AI of class `cls`
        Right(game.game.gameConfiguration.playersInfo.values.collectFirst {
          case PlayerInfo(playerName, _, _, _, PlayerType.ArtificialIntelligence) =>
            Event.PlayerRemoved(playerName.name)
        })
      case (_, Open(game, _)) => decideWhenOpen(game, id).map(Some(_))
      case _                             => Left(ErrorADT.GameDoesNotExist(id))
    }

    private def decideWhenOpen(game: MenuGameWithPlayers, id: Int): Either[ErrorADT, Event] = this match {
      case Join(user, maybePassword, _) =>
        Either.cond(
          game.game.maybeHashedPassword.forall(hashed =>
            maybePassword.exists(Crypto.checkPassword(_, HashedPassword(hashed)))
          ),
          Event.PlayerJoined(user),
          ErrorADT.PasswordsMismatch
        )
      case RemovePlayer(playerName, _) => Right(Event.PlayerRemoved(playerName))
      case ChangePlayerInfo(user, playerInfo, _) =>
        Either.cond(game.containsPlayer(user), Event.PlayerInfoChanged(playerInfo), ErrorADT.YouAreNotInGame(id))
      case ChangeMetadata(requester, metadata, _) =>
        Either.cond(
          game.isGameCreator(requester),
          Event.MetadataChanged(metadata),
          ErrorADT.YouAreNotCreator(requester.name)
        )
      case AddAI(_) => nextAI(game).map(Event.AIAdded(_))
      case Launch(requester, _) =>
        for {
          _ <- Either.cond(!game.started, (), ErrorADT.GameAlreadyLaunched(id))
          _ <- Either.cond(game.isGameCreator(requester), (), ErrorADT.YouAreNotCreator(requester.name))
          gameCredentials = GameCredentials(id, java.util.UUID.randomUUID().toString)
          userCredentials = game.players.map { user =>
            GameUserCredentials(user.name, id, java.util.UUID.randomUUID().toString)
          }
        } yield Event.GameLaunched(
          GameCredentialsWithGameInfo(AllGameCredentials(gameCredentials, userCredentials), game.start)
        )
      case CancelLaunch(_)             => Right(Event.LaunchCancelled())
      case End(_)                      => Right(Event.GameEnded())
      case Create(_, _) | Get(_) | RemoveAI(_, _) => Left(ErrorADT.RawInternalError("unreachable"))
    }

  enum Event derives Codec:
    case GameCreated(game: MenuGameWithPlayers)
    case PlayerJoined(user: User)
    case PlayerRemoved(playerName: String)
    case PlayerInfoChanged(playerInfo: PlayerInfo)
    case AIAdded(playerInfo: PlayerInfo)
    case MetadataChanged(metadata: GameConfigMetadata)
    case GameLaunched(credentials: GameCredentialsWithGameInfo)
    case LaunchCancelled()
    case GameEnded()

    def apply(state: MenuGameEntity): MenuGameEntity = (this, state) match {
      case (GameCreated(game), _)                => Open(game, None)
      case (GameEnded(), _)                      => Over(state.maybeCredentials)
      case (event, Open(game, credentials))      => event.applyWhenOpen(game, credentials)
      case _                                     => state
    }

    private def applyWhenOpen(
        game: MenuGameWithPlayers,
        credentials: Option[GameCredentialsWithGameInfo]
    ): MenuGameEntity = this match {
      case PlayerJoined(user) =>
        Open(
          game.copy(
            players = game.players :+ user,
            game = game.game.withPlayer(Pointed[PlayerInfo].unit.withHumanName(user.name))
          ),
          credentials
        )
      case PlayerRemoved(playerName)     => Open(game.removePlayer(playerName), credentials)
      case PlayerInfoChanged(playerInfo) => Open(game.withPlayer(playerInfo), credentials)
      case AIAdded(playerInfo)           => Open(game.withPlayer(playerInfo), credentials)
      case MetadataChanged(metadata) =>
        val bossChanged = game.game.gameConfiguration.bossName != metadata.bossName
        val updatedGame = game.copy(game =
          game.game.copy(gameConfiguration = game.game.gameConfiguration.withMetadata(metadata))
        )
        Open(if bossChanged then updatedGame.removeAllAIs else updatedGame, credentials)
      case GameLaunched(newCredentials) => Open(game.start, Some(newCredentials))
      case LaunchCancelled()            => Open(game.copy(started = false), credentials)
      case GameCreated(_) | GameEnded() => Open(game, credentials) // handled in `apply`
    }

  /** The "next" AI of the game's boss.
    *
    * @return
    *   an error if the AI could not be added (mostly because full, or boss has no ai implemented yet)
    */
  private def nextAI(game: MenuGameWithPlayers): Either[ErrorADT, PlayerInfo] = for {
    bossMetadata <- BossMetadata
      .maybeMetadataByName(game.game.gameConfiguration.bossName)
      .toRight(
        ErrorADT.BossDoesNotExist(s"Boss ${game.game.gameConfiguration.bossName} does not exist")
      )
    _ <- bossMetadata.maybeAIComposition
      .toRight(ErrorADT.BossDoesNotHaveAIImplemented(bossMetadata.name))

    players        = game.game.gameConfiguration.playersInfo.values.toVector
    currentClasses = players.flatMap(_.maybePlayerClass)
    aiClass <- bossMetadata.nextAvailableAI(currentClasses).toRight(ErrorADT.NoMoreAIAvailable())
    aiIndicesWithThisClass = players
      .map(_.playerName)
      .collect {
        case PlayerName.AIPlayerName(cls, index) if cls == aiClass => index
      }
      .sorted
    name = aiIndicesWithThisClass.zipWithIndex
      .collectFirst {
        case (aiIndex, indexInVector) if aiIndex != indexInVector => indexInVector
      }
      .map(PlayerName.AIPlayerName(aiClass, _))
      .getOrElse(PlayerName.AIPlayerName(aiClass, aiIndicesWithThisClass.length))
  } yield PlayerInfo(
    name,
    Some(aiClass),
    Some(
      RGBColour.someColours
        .find(colour => !players.flatMap(_.maybePlayerColour).contains[RGBColour](colour))
        .getOrElse(RGBColour.someColours.head)
    ),
    PlayerStatus.Ready,
    PlayerType.ArtificialIntelligence
  )

  val entityInfo: EntityInformation[Command, Event, MenuGameEntity] =
    EntityInformation.usingCirceSerialization[Command, Event, MenuGameEntity](
      NotCreated,
      _(_),
      (command, state, id) => command.handle(state, id)
    )

}
