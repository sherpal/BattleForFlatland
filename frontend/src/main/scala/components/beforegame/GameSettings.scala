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
import menus.data.DataUpdated
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
        _ <- ZIO.when(result.isRight)(services.localstorage.storeAt("player-info", info).ignore)
      } yield result
    )

    val playerUpdateFailures = playerUpdateEvents.collect { case Left(error) => error }

    val gameConfigUpdateBus = new EventBus[GameConfiguration.GameConfigMetadata]
    val gameConfigUpdateEvents = gameConfigUpdateBus.events.flatMapSwitchZIO(gameConfig =>
      for {
        gameConfigChanged <- services.menugames.changeGameConfig(gameId, gameConfig)
        _                 <- services.localstorage.storeAt("game-config", gameConfig).ignore
      } yield gameConfigChanged
    )

    val dataRefreshSocket = JsonWebSocket[DataUpdated, Unit, String](
      models.bff.Routes.gameJoinedWS,
      models.bff.Routes.gameIdParam,
      gameId
    )
    val dataRefreshEvents = EventStream.merge(
      dataRefreshSocket.openEvents.mapToUnit,
      dataRefreshSocket.inEvents.debugLog(_ => true).mapToUnit,
      playerUpdateFailures.mapToUnit
    )

    val maybeDataRefreshEvents =
      dataRefreshEvents.flatMapSwitchZIO(_ => menugames.gameInfo(gameId).either)

    val gameDoesNotExistAnymoreEvents = maybeDataRefreshEvents.collect { case Left(err) => err }
    val gameDataEvents                = maybeDataRefreshEvents.collect { case Right(data) => data }
    val playerNotInGameEvents         = gameDataEvents.map(_.containsPlayer(user)).map(!_)

    def playerUpdateFailureDialog = div(
      Dialog(
        _.open <-- playerUpdateFailures.mapTo(true),
        IllustratedMessage(
          _.name          := IllustratedMessageType.ErrorScreen,
          _.titleText     := "Error while changing your settings",
          _.subtitleText <-- playerUpdateFailures.startWithNone.map(_.fold("")(_.repr))
        )
      )
    )

    def gameDeletedDialog = {
      val closeBus = new EventBus[Unit]
      val problemEvents = EventStream.merge["game-removed" | "not-in-game"](
        gameDoesNotExistAnymoreEvents.mapTo("game-removed"),
        playerNotInGameEvents.filter(identity).mapTo("not-in-game")
      )
      Dialog(
        _.open <-- EventStream.merge(problemEvents.mapTo(true), closeBus.events.mapTo(false)),
        _.slots.header := Bar(_.slots.startContent := Title.h2("Game was closed.")),
        Text(
          child.text <-- problemEvents.map {
            case "game-removed" =>
              s"The game ($gameId) was closed by its creator (or a cataclysmic effect removed it from the database)."
            case "not-in-game" =>
              "You have been kicked out of the game (or maybe you were never in it and end up here by mistake)."
          }
        ),
        Text(marginTop := "0.5em", "You will be redirected to the menus."),
        _.slots.footer := Bar(
          _.slots.endContent := Button(
            "Back to menu",
            _.events.onClick.mapToUnit --> closeBus.writer,
            _.design := ButtonDesign.Emphasized
          )
        ),
        _.events.onClose.mapTo(()) --> Observer.fromZIO[Any](_ =>
          zio.Console.printLine("back to menus...").orDie *> services.routing.moveTo(
            MainPage.allGames
          )
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
                  services.localstorage
                    .retrieveFrom[GameConfiguration.GameConfigMetadata]("game-config")
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
                TableCompat.row.cell(name.name),
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

    div(
      dataRefreshSocket.modifier,
      s"Settings for game $gameId",
      gameMetadata,
      displayPlayers,
      readyToGo,
      Bar(
        _.slots.endContent := leaveGameButton,
        _.slots.endContent <-- gameDataEvents.filter(_.game.gameCreator == user).map { game =>
          Button(
            _.disabled := !game.game.gameConfigurationIsValid,
            "Launch game!",
            _.events.onClick --> Observer[Any](_ => println("The game should launch!")),
            _.design := ButtonDesign.Positive
          )
        }
      ),
      gameDeletedDialog,
      playerUpdateFailureDialog,
      onMountZIO(for {
        storedPlayerInfo <- services.localstorage
          .retrieveFrom[PlayerInfo]("player-info")
          .catchAll(t => Console.printError(t).ignore.as(None))
        maybePlayerInfoToSet = storedPlayerInfo.map(_.withReadyState(NotReady))
        _ <- maybePlayerInfoToSet match {
          case None             => ZIO.unit
          case Some(playerInfo) => ZIO.succeed(playerUpdateBus.writer.onNext(playerInfo))
        }
      } yield ()),
      dataRefreshSocket.closedSignal.changes.filter(identity).mapToUnit --> refreshNeedObs
    )
  }

}
