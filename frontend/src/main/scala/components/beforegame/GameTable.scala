package components.beforegame

import com.raquo.laminar.api.L.*
import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.compat.*
import zio.Runtime
import services.FrontendEnv
import utils.laminarzio.*
import menus.data.User
import be.doeraene.webcomponents.ui5.configkeys.MessageStripDesign
import models.bff.outofgame.MenuGameWithPlayers
import be.doeraene.webcomponents.ui5.configkeys.IconName
import scala.languageFeature.experimental
import be.doeraene.webcomponents.ui5.configkeys.ButtonDesign
import utils.websocket.JsonWebSocket
import menus.data.DataUpdated
import services.menugames.{menuGames, createGame}
import be.doeraene.webcomponents.ui5.configkeys.ValueState

object GameTable {

  def apply(user: User)(using Runtime[FrontendEnv]): HtmlElement = {
    val dataRefreshSocket = JsonWebSocket[DataUpdated, Unit](
      models.bff.Routes.gameJoinedWS
    )
    val dataRefreshEvents = EventStream.merge(
      dataRefreshSocket.openEvents.mapToUnit,
      dataRefreshSocket.inEvents.debugLog(_ => true).mapToUnit
    )
    val dataSignal =
      dataRefreshEvents.flatMapSwitchZIO(_ => services.menugames.menuGames).startWithNone
    val gameYouAreInSignal =
      dataSignal.map(_.map(_.find(_.containsPlayer(user))))

    val joinGameBus = new EventBus[(MenuGameWithPlayers, Boolean)]

    val joinGameUrl =
      (services.routing.base / models.bff.Routes.inGame) ? models.bff.Routes.gameIdParam

    val joinNewGameEvents = joinGameBus.events
      .collect { case (game, false) => game }
      .flatMapSwitchZIO(game => services.menugames.joinGame(game.id).map(_.map(_ => game.id)))
    val joinGameBusySignal = EventStream
      .merge(
        joinGameBus.events.mapTo(true),
        joinNewGameEvents.mapTo(false)
      )
      .startWith(false)

    def youAreInGameMessage = div(child.maybe <-- gameYouAreInSignal.map(_.flatten).map(_.map { gameYouAreIn =>
      MessageStrip(
        _.hideCloseButton := true,
        _.design          := MessageStripDesign.Critical,
        s"You are currently involved in a game. Join it back by going ",
        components.router.Link(joinGameUrl, gameYouAreIn.id)("here"),
        "."
      )
    }))

    def createGameBar = {
      val openPopoverBus = new EventBus[Boolean]
      val createGameButtonId = "create-game-btn"
      val gameNameVar = Var("")
      val createGameClickBus = new EventBus[Unit]

      val createGameEvents = createGameClickBus.events.sample(gameNameVar.signal)
        .flatMapSwitchZIO(gameName => createGame(gameName))

      val createGameOnFlightSignal = EventStream.merge(
        createGameClickBus.events.mapTo(true),
        createGameEvents.mapTo(false)
      ).startWith(false)

      val errorMessageSignal = EventStream.merge(
        gameNameVar.signal.changes.mapTo(None),
        createGameEvents.map(_.swap.toOption)
      ).startWith(None)

      val createdGameEvents = createGameEvents.collect {
        case Right(game) => game
      }

      Bar(
        _.slots.startContent := Button(
          idAttr := createGameButtonId, 
          _.events.onClick.mapTo(true) --> openPopoverBus.writer,
          _.design := ButtonDesign.Emphasized,
          "Create game"
        ),
        Popover(
          _.openerId := createGameButtonId,
          _.open <-- openPopoverBus.events,
          Label("Choose a name"),
          Input(
            _.placeholder := "My Game", 
            _.events.onInput.map(_.target.value) --> gameNameVar.writer, 
            onMountFocus,
            _.valueState <-- errorMessageSignal.map {
              case Some(_) => ValueState.Negative
              case None    => ValueState.None
            },
            _.slots.valueStateMessage <-- errorMessageSignal.map {
              case None => Label()
              case Some(error) => Label(error.repr)
            }
          ),
          Button(
            marginLeft := "0.5em",
            _.disabled <-- createGameOnFlightSignal,
            _.iconOnly := true,
            _.design := ButtonDesign.Emphasized,
            _.icon := IconName.play,
            _.events.onClick.mapToUnit --> createGameClickBus.writer
          )
        ),
        createdGameEvents.map(_.id) --> Observer.fromZIO[String](gameId =>
          services.routing.moveTo(services.routing.base / models.bff.Routes.inGame, models.bff.Routes.gameIdParam)(gameId)
        )
      )
    }

    div(
      youAreInGameMessage,
      child.maybe <-- gameYouAreInSignal.map(_.exists(_.isEmpty)).map(Option.when(_)(createGameBar)),
      TableCompat(
        _.noDataText    := "There are currently no games. Go ahead and create one!",
        _.hideNoData    := false,
        _.busy         <-- dataSignal.map(_.isEmpty),
        _.slots.columns := TableCompat.column("Id", _.minWidth := 200),
        _.slots.columns := TableCompat.column("Name"),
        _.slots.columns := TableCompat.column("Created By"),
        _.slots.columns := TableCompat.column("Number of players"),
        _.slots.columns := TableCompat.column("Actions"),
        children <-- dataSignal
          .map(_.getOrElse(Vector.empty))
          .map(_.map { game =>
            TableCompat.row(
              _.cell(game.id),
              _.cell(game.game.gameName),
              _.cell(game.game.gameCreator.name),
              _.cell(game.players.length.toInt),
              _.cell(
                child.maybe <-- gameYouAreInSignal.map(maybeGame =>
                  def button(alreadyIn: Boolean) = Some(
                        Button(
                          _.iconOnly := true,
                          _.icon     := IconName.`journey-arrive`,
                          _.events.onClick.mapTo((game, alreadyIn)) --> joinGameBus.writer,
                          _.disabled <-- joinGameBusySignal,
                          _.design := ButtonDesign.Emphasized
                        )
                      )
                  maybeGame match {
                    case Some(None) => // we are not in a game, we may join any game
                      button(false)
                    case Some(Some(gameYouAreIn)) if game.id == gameYouAreIn.id =>
                      // we are in this particular game, we may join it
                      button(true)
                    case Some(Some(_)) => 
                      // we are in a game but not this one, we may not join it
                      None
                    case None => // initial data not yet loaded
                      None
                  }
                )
              )
            )
          })
      ),
      EventStream.merge(
        joinGameBus.events.collect { case (game, true) => game.id },
        joinNewGameEvents.collect { case Right(gameId) => gameId }
      ) --> Observer.fromZIO[String](services.routing.moveTo(joinGameUrl)),
      dataRefreshSocket.modifier
    )
  }

}
