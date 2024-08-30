package components.beforegame

import com.raquo.laminar.api.L.*
import be.doeraene.webcomponents.ui5.*
import zio.ZIO
import services.FrontendEnv
import menus.data.User
import utils.laminarzio.fromZIO
import utils.laminarzio.onMountZIO
import urldsl.language.dummyErrorImpl.*
import models.bff.outofgame.MenuGameWithPlayers
import io.circe.Encoder
import menus.data.APIResponse
import components.router.Routes
import components.router.Route
import components.Redirect
import be.doeraene.webcomponents.ui5.configkeys.IllustratedMessageType

object MainPage {

  def apply(
      checkMe: ZIO[FrontendEnv, Nothing, Option[User]]
  )(using zio.Runtime[FrontendEnv]): HtmlElement =
    div(
      child <-- EventStream.fromZIO(checkMe).map {
        case None => div(onMountZIO(services.routing.moveTo(models.users.Routes.login)))
        case Some(user) =>
          div(
            Title.h1("Battle for Flatland"),
            Text(s"Welcome, ${user.name}"),
            child <-- Routes
              .firstOf(
                Route(
                  allGames / endOfSegments,
                  () => GameTable(user)
                ),
                Route(
                  (services.routing.base / models.bff.Routes.inGame) ? models.bff.Routes.gameIdParam,
                  (_, gameId) => GameSettings.withReconnects(user, gameId)
                ),
                Route(
                  services.routing.base / endOfSegments,
                  () => Redirect(allGames)
                ),
                Route(
                  services.routing.base / models.bff.Routes.bff / endOfSegments,
                  () => Redirect(allGames)
                )
              )
              .map(
                _.getOrElse(
                  IllustratedMessage(
                    _.name      := IllustratedMessageType.Tent,
                    _.titleText := "There are no page here.",
                    _.slots.subtitle := Text(
                      "Go back ",
                      components.router.Link(allGames)("here"),
                      "."
                    )
                  )
                )
              )
          )
      }
    )

  def allGames = services.routing.base / models.bff.Routes.allGames

}
