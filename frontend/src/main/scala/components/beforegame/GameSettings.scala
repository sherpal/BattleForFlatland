package components.beforegame

import com.raquo.laminar.api.L.*
import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.compat.*
import menus.data.User
import services.FrontendEnv
import services.menugames
import utils.laminarzio.*
import be.doeraene.webcomponents.ui5.configkeys.ButtonDesign
import scala.scalajs.js
import utils.websocket.JsonWebSocket
import menus.data.MenuGameComm
import com.raquo.airstream.ownership.Owner
import models.bff.outofgame.MenuGameWithPlayers
import be.doeraene.webcomponents.ui5.configkeys.IconName
import be.doeraene.webcomponents.ui5.scaladsl.colour.Colour
import be.doeraene.webcomponents.ui5.configkeys.TableMode
import models.bff.outofgame.gameconfig.PlayerInfo
import utils.misc.RGBColour
import be.doeraene.webcomponents.ui5.configkeys.IllustratedMessageType
import models.bff.outofgame.PlayerClasses
import models.bff.outofgame.gameconfig.PlayerStatus
import gamelogic.docs.BossMetadata
import models.bff.outofgame.gameconfig.GameConfiguration
import zio.*
import models.bff.outofgame.gameconfig.PlayerStatus.Ready
import models.bff.outofgame.gameconfig.PlayerStatus.NotReady
import errors.ErrorADT
import java.util.concurrent.TimeUnit
import models.bff.ingame.GameUserCredentials
import services.localstorage.LocalStorage
import gamelogic.entities.boss.Boss101
import models.bff.outofgame.gameconfig.PlayerType
import models.bff.outofgame.gameconfig.PlayerName.HumanPlayerName
import models.bff.outofgame.gameconfig.PlayerName.AIPlayerName

object GameSettings {

  def withReconnects(user: User, gameId: String)(using Runtime[FrontendEnv]): HtmlElement = {
    val refreshNeededBus = new EventBus[Unit]

    div(
      child <-- refreshNeededBus.events
        .tapEach(_ => println("Need refreshing, doing in 5..."))
        .delay(5000)
        .startWith(())
        .mapTo(
          apply(user, gameId, refreshNeededBus.writer)
        )
    )
  }

  def apply(user: User, gameId: String, refreshNeedObs: Observer[Unit])(using
      zio.Runtime[FrontendEnv]
  ): HtmlElement = {
    val playerUpdateBus = new EventBus[PlayerInfo]
    val playerUpdateEvents = playerUpdateBus.events.flatMapSwitchZIO(info =>
      for {
        result <- services.menugames.changePlayerInfo(gameId, info)
        _      <- ZIO.when(result.isRight)(playerInfoKey.store(info).ignore)
      } yield result
    )

    val playerUpdateFailures = playerUpdateEvents.collect { case Left(error) => error }

    val launchGameBus    = new EventBus[Unit]
    val launchGameEvents = launchGameBus.events.flatMapSwitchZIO(_ => menugames.launchGame(gameId))
    val launchGameFailuresEvents = launchGameEvents.collect { case Left(err) => err }
    val gameIsLaunchingSignal = EventStream
      .merge(
        launchGameBus.events.mapTo(true),
        launchGameFailuresEvents.mapTo(false)
      )
      .startWith(false)

    val gameConfigUpdateBus = new EventBus[GameConfiguration.GameConfigMetadata]
    val gameConfigUpdateEvents = gameConfigUpdateBus.events.flatMapSwitchZIO(gameConfig =>
      for {
        gameConfigChanged <- services.menugames.changeGameConfig(gameId, gameConfig)
        _                 <- gameConfigKey.store(gameConfig).ignore
      } yield gameConfigChanged
    )

    val dataRefreshSocket = JsonWebSocket[MenuGameComm, Unit, String](
      models.bff.Routes.gameJoinedWS,
      models.bff.Routes.gameIdParam,
      gameId
    )
    val dataRefreshEvents = EventStream.merge(
      dataRefreshSocket.openEvents.mapToUnit,
      dataRefreshSocket.inEvents.collect { case MenuGameComm.DataUpdated() => () },
      playerUpdateFailures.mapToUnit
    )
    val gameIsStartingSignal = dataRefreshSocket.inEvents
      .collect { case MenuGameComm.GameStarted() =>
        true
      }
      .startWith(false)

    val maybeDataRefreshEvents =
      dataRefreshEvents.flatMapSwitchZIO(_ => menugames.gameInfo(gameId).either)

    val gameDoesNotExistAnymoreEvents = maybeDataRefreshEvents.collect { case Left(err) => err }
    val gameDataEvents                = maybeDataRefreshEvents.collect { case Right(data) => data }
    val playerNotInGameEvents         = gameDataEvents.map(_.containsPlayer(user)).map(!_)

    def playerUpdateFailureDialog =
      val closeBus = new EventBus[Unit]
      div(
        Dialog(
          _.open <-- EventStream.merge(
            playerUpdateFailures
              .filter {
                case _: ErrorADT.GameDoesNotExist => false
                case _                            => true
              }
              .mapTo(true),
            closeBus.events.mapTo(false)
          ),
          IllustratedMessage(
            _.name          := IllustratedMessageType.ErrorScreen,
            _.titleText     := "Error while changing your settings",
            _.subtitleText <-- playerUpdateFailures.startWithNone.map(_.fold("")(_.repr))
          ),
          _.slots.footer := Bar(
            _.slots.endContent := Button("Close", _.events.onClick.mapToUnit --> closeBus.writer)
          )
        )
      )

    def gameProblemsDialog = {
      val closeBus = new EventBus[Unit]
      sealed trait Problem
      case object GameRemoved                   extends Problem
      case object NotInGame                     extends Problem
      case class GameLaunchIssue(err: ErrorADT) extends Problem
      val problemEvents = EventStream.merge[Problem](
        gameDoesNotExistAnymoreEvents.mapTo(GameRemoved),
        playerNotInGameEvents.filter(identity).mapTo(NotInGame),
        launchGameFailuresEvents.map(GameLaunchIssue(_))
      )
      Dialog(
        _.open <-- EventStream.merge(problemEvents.mapTo(true), closeBus.events.mapTo(false)),
        _.slots.header := Bar(
          _.slots.startContent := Title.h2(
            child.text <-- problemEvents.map {
              case GameRemoved        => "Game does not exist (anymore)"
              case NotInGame          => "You should not be here"
              case _: GameLaunchIssue => "Game Launch issue 😱"
            }
          )
        ),
        child <-- problemEvents.map {
          case GameRemoved =>
            div(
              Text(
                s"The game ($gameId) was closed by its creator (or a cataclysmic effect removed it from the database). Or perhaps it never existed, who knows?"
              ),
              Text(marginTop := "0.5em", "You will be redirected to the menus.")
            )
          case NotInGame =>
            div(
              Text(
                "You have been kicked out of the game (or maybe you were never in it and end up here by mistake)."
              ),
              Text(marginTop := "0.5em", "You will be shown the way out towards the menus.")
            )
          case GameLaunchIssue(err) =>
            div(Text("There was an issue."), pre(err.repr))
        },
        _.slots.footer := Bar(
          _.slots.endContent <-- problemEvents.map {
            case NotInGame | GameRemoved =>
              Button(
                "Back to menu",
                _.events.onClick.mapToUnit --> closeBus.writer,
                _.design := ButtonDesign.Emphasized,
                _.events.onClick.mapToUnit --> Observer.fromZIO[Any](_ =>
                  services.routing.moveTo(
                    MainPage.allGames
                  )
                )
              )
            case GameLaunchIssue(err) =>
              Button(
                "Close",
                _.events.onClick.mapToUnit --> closeBus.writer,
                _.design := ButtonDesign.Emphasized
              )
          }
        )
      )
    }

    // boss selection essentially
    def gameMetadata = div(
      child <-- gameDataEvents.distinct.map { game =>
        val amICreator      = game.game.gameCreator == user
        val currentConfig   = game.game.gameConfiguration
        val currentBossName = currentConfig.bossName
        div(
          div(
            Label("Boss", marginRight := "0.7em"),
            if amICreator then
              Select(
                _.events.onChange
                  .map(_.detail.selectedOption.maybeValue)
                  .map(_.flatMap(BossMetadata.maybeMetadataByName))
                  .map(_.get.name)
                  .map(currentConfig.withBossName)
                  .map(_.metadata) --> gameConfigUpdateBus.writer,
                gameConfigUpdateEvents --> Observer.empty,
                BossMetadata.allBossNames.map(bossName =>
                  Select.option(
                    _.value := bossName,
                    bossName,
                    _.selected := (currentBossName == bossName)
                  )
                ),
                onMountZIO(
                  gameConfigKey.retrieve
                    .catchAll(_ => ZIO.none)
                    .map(_.filter(_ != currentConfig.metadata))
                    .flatMap {
                      case None => ZIO.unit
                      case Some(metadata) =>
                        ZIO.succeed(gameConfigUpdateBus.writer.onNext(metadata))
                    }
                )
              )
            else Label(currentBossName)
          )
        )
      }
    )

    val controlSettingsOpenerBus = new EventBus[Boolean]
    def controlSettingsDialog = {
      val resetBus = new EventBus[Unit]
      Dialog(
        _.showFromEvents(controlSettingsOpenerBus.events.filter(identity).mapToUnit),
        _.closeFromEvents(controlSettingsOpenerBus.events.filterNot(identity).mapToUnit),
        _.slots.header := Bar(_.slots.startContent := Title.h2("Control Settings")),
        ControlSettings(resetBus.events),
        _.slots.footer := Bar(
          _.slots.endContent := Button("Reset", _.events.onClick.mapToUnit --> resetBus.writer),
          _.slots.endContent := Button(
            "Close",
            _.design := ButtonDesign.Emphasized,
            _.events.onClick.mapTo(false) --> controlSettingsOpenerBus.writer
          )
        )
      )
    }

    def displayPlayers: HtmlElement = TableCompat(
      _.mode          := TableMode.None,
      _.slots.columns := TableCompat.column("Name"),
      _.slots.columns := TableCompat.column("Class"),
      _.slots.columns := TableCompat.column("Colour"),
      _.slots.columns := TableCompat.column("Ready"),
      _.slots.columns <-- gameDataEvents
        .map(_.game.gameCreator == user)
        .map(Option.when(_)(TableCompat.column("Actions")).toSeq),
      children <-- gameDataEvents
        .map(game =>
          game.game.gameConfiguration.playersInfo.values.toVector
            .map(info => (info, info.playerName.name == user.name, user == game.game.gameCreator))
            .zipWithIndex
            .sortBy {
              case ((_, true, _), _) => -1
              case (_, index)        => index
            }
            .map(_._1)
        )
        .split(_._1.playerName) { case (name, (_, isMe, amIGameCreator), playerInfoWithIsMeSignal) =>
          val playerInfoSignal = playerInfoWithIsMeSignal.map(_._1)
          TableCompat.row(
            children <-- playerInfoSignal.map { playerInfo =>
              val ui5Colour = playerInfo.maybePlayerColour
                .map(_.intColour)
                .map(Colour.fromIntColour)
                .getOrElse(Colour.white)
              Vector(
                TableCompat.row.cell(name match
                  case HumanPlayerName(name) => name
                  case ai: AIPlayerName      => s"🤖 ${ai.name}"
                ),
                TableCompat.row.cell(
                  if isMe then
                    Select(
                      _.events.onChange
                        .map(
                          _.detail.selectedOption.maybeValue
                            .flatMap(PlayerClasses.playerClassByName)
                        )
                        .map(playerInfo.withClass) --> playerUpdateBus.writer,
                      _.option(
                        ""
                      ),
                      PlayerClasses.allChoices.map { cls =>
                        Select.option(
                          _.value := cls.value,
                          cls.value,
                          _.selected := playerInfo.maybePlayerClass.contains(cls)
                        )
                      }
                    )
                  else playerInfo.maybePlayerClass.map(_.toString).getOrElse("Not Chosen")
                ),
                TableCompat.row.cell(
                  width := "3em",
                  if isMe then
                    val openerId       = "palette-opener-id"
                    val openPopoverBus = new EventBus[Boolean]
                    span(
                      ColourPalette(
                        _.item(_.value := ui5Colour, idAttr := openerId),
                        _.events.onItemClick.mapTo(true) --> openPopoverBus.writer
                      ),
                      ColourPalettePopover(
                        _.openerId        := openerId,
                        _.open           <-- openPopoverBus.events,
                        _.showMoreColours := true,
                        Colour.someColours.map(c => ColourPalette.item(_.value := c)),
                        _.events.onItemClick
                          .map(_.detail.scalaColour.intColour)
                          .map(RGBColour.fromIntColour)
                          .map(playerInfo.withColour) --> playerUpdateBus.writer
                      )
                    )
                  else
                    span(
                      padding := "11px",
                      span(
                        height          := "2em",
                        width           := "2em",
                        backgroundColor := ui5Colour.rgb,
                        borderRadius    := "50%",
                        border := s"1px solid ${if ui5Colour.isBright then "black" else "gray"}",
                        display.inlineBlock
                      )
                    )
                ),
                TableCompat.row.cell(
                  if isMe then
                    Switch(
                      _.checked := playerInfo.isReady,
                      _.events.onCheckedChange
                        .map(if _ then PlayerStatus.Ready else PlayerStatus.NotReady)
                        .map(playerInfo.withReadyState) --> playerUpdateBus.writer
                    )
                  else
                    Icon(
                      _.name := (if playerInfo.isReady then IconName.accept else IconName.decline),
                      color  := (if playerInfo.isReady then "green" else "red")
                    )
                )
              ) ++ Option
                .when(amIGameCreator && !isMe)(
                  TableCompat.row.cell(
                    Button(
                      _.tooltip  := s"Remove ${playerInfo.playerName.name} from this game",
                      _.iconOnly := true,
                      _.icon     := IconName.delete,
                      _.events.onClick --> Observer.fromZIO[Any](_ =>
                        services.menugames.kickPlayer(gameId, playerInfo.playerName.name).unit
                      )
                    )
                  )
                )
                .toVector
            }
          )
        }
    )

    def leaveGameButton = Button(
      "Leave Game",
      _.events.onClick.mapToUnit --> Observer.fromZIO[Any](_ =>
        for {
          _ <- menugames.leaveGame(gameId)
          _ <- services.routing.moveTo(MainPage.allGames)
        } yield ()
      )
    )

    def readyToGo = {
      val gameConfigErrors = gameDataEvents.map(_.game.gameConfiguration.validate).map(_.toVector)
      div(
        Text(
          child.text <-- gameConfigErrors
            .map(_.isEmpty)
            .map(
              if _ then "Everyone is ready and well set, LFG"
              else "The following things prevent the game to start:"
            )
        ),
        Tree(children <-- gameConfigErrors.split(_._1) { case (name, _, errorsWithNameSignal) =>
          val errorsSignal = errorsWithNameSignal.map(_._2)
          Tree.item(
            _.text    := name,
            children <-- errorsSignal.map(errors => errors.map(error => Tree.item(_.text := error)))
          )
        })
      )
    }

    def launchGameButton(toastObserver: Observer[String])(game: MenuGameWithPlayers) =
      Button(
        _.disabled <-- gameIsLaunchingSignal,
        "Launch game!",
        _.events.onClick
          .filter(_ => !game.game.gameConfigurationIsValid)
          .mapTo("Can't start the game, check remaining issues above") --> toastObserver,
        _.events.onClick
          .filter(_ => game.game.gameConfigurationIsValid)
          .mapToUnit --> launchGameBus.writer,
        _.design := ButtonDesign.Positive
      )

    def addAIButton = {
      val bossMetadataSignal = gameDataEvents
        .map(_.game.gameConfiguration.bossName)
        .map(BossMetadata.maybeMetadataByName)
        .collect { case Some(bossMetadata) =>
          bossMetadata
        }
        .startWith(Boss101)
      val clickBus = new EventBus[Unit]
      Button(
        _.disabled <-- gameIsLaunchingSignal
          .combineWith(bossMetadataSignal.map(_.maybeAIComposition.isEmpty))
          .map(_ || _),
        "Add AI",
        _.events.onClick.mapToUnit --> clickBus.writer,
        clickBus.events.flatMapSwitchZIO(_ => menugames.addAIToGame(gameId)) --> Observer.empty
      )
    }

    // emit to this bus to show a toast. use sparsely
    val toastBus = new EventBus[String]

    div(
      controlSettingsDialog,
      width := "100%",
      BusyIndicator(
        width     := "100%",
        _.active <-- gameIsStartingSignal,
        _.delay   := scala.concurrent.duration.FiniteDuration(10, TimeUnit.MILLISECONDS),
        _.text    := "Game is starting, please wait for setup to finish...",
        div(
          width := "100%",
          Toast(_.showFromTextEvents(toastBus.events)),
          dataRefreshSocket.modifier,
          s"Settings for game $gameId",
          gameMetadata,
          displayPlayers,
          readyToGo,
          Bar(
            _.slots.endContent := Button(
              "Control Settings",
              _.events.onClick.mapTo(true) --> controlSettingsOpenerBus.writer
            ),
            _.slots.endContent := leaveGameButton,
            _.slots.endContent <-- gameDataEvents
              .filter(_.isGameCreator(user))
              .map(_ => addAIButton),
            _.slots.endContent <-- gameDataEvents
              .filter(_.game.gameCreator == user)
              .map(launchGameButton(toastBus.writer))
          ),
          gameProblemsDialog,
          playerUpdateFailureDialog,
          onMountZIO(for {
            storedPlayerInfo <- playerInfoKey.retrieve
              .catchAll(t => Console.printError(t).ignore.as(None))
            maybePlayerInfoToSet = storedPlayerInfo.map(_.withReadyState(NotReady))
            _ <- maybePlayerInfoToSet match {
              case None             => ZIO.unit
              case Some(playerInfo) => ZIO.succeed(playerUpdateBus.writer.onNext(playerInfo))
            }
          } yield ()),
          dataRefreshSocket.closedSignal.changes.filter(identity).mapToUnit --> refreshNeedObs,
          dataRefreshSocket.inEvents.collect {
            case MenuGameComm.HereAreYourCredentials(gameId, secret, port) =>
              (GameUserCredentials(user.name, gameId, secret), port)
          } --> Observer.fromZIO[(GameUserCredentials, Int)]((creds, port) =>
            for {
              _ <- ZIO.succeed(println(creds))
              _ <- credentialsStorageKey.store(creds).orDie
              _ <- gamePortKey.store(port).orDie
              _ <- services.routing.moveTo(
                (services.routing.base / models.bff.Routes.inGame) ? models.bff.Routes.gameIdParam
              )(gameId)
            } yield ()
          )
        )
      )
    )
  }

  val credentialsStorageKey = LocalStorage.key[GameUserCredentials]("game-credentials")
  val gamePortKey           = LocalStorage.key[Int]("game-server-port")

  private val playerInfoKey = LocalStorage.key[PlayerInfo]("player-info")
  private val gameConfigKey = LocalStorage.key[GameConfiguration.GameConfigMetadata]("game-config")

}
