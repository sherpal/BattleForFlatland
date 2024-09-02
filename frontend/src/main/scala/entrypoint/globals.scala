package entrypoint

import com.raquo.laminar.api.L.*
import be.doeraene.webcomponents.ui5.*
import org.scalajs.dom
import zio.*
import scala.util.Success
import scala.util.Failure
import be.doeraene.webcomponents.ui5.configkeys.MessageStripDesign
import menus.data.User
import components.router.Routes
import components.router.Route
import utils.laminarzio.*
import be.doeraene.webcomponents.ui5.configkeys.IllustratedMessageType
import services.FrontendEnv
import components.login.Login
import components.beforegame.MainPage
import services.routing
import components.duringgame.DuringGameMainPage

@main def run(): Unit = {
  println("Hello Battle for Flatland!")

  val layer = ZLayer.make[services.FrontendEnv](
    services.http.FHttpClient.live,
    routing.FRouting.live,
    services.errorreporting.ErrorReporting.live,
    services.menugames.MenuGames.live,
    services.localstorage.FLocalStorage.live
  )

  val runtimeF = Unsafe.unsafe { implicit unsafe =>
    Runtime.default.unsafe.runToFuture(ZIO.runtime[services.FrontendEnv].provideLayer(layer))
  }

  runtimeF.onComplete {
    case Success(runtime) =>
      import urldsl.language.PathSegment.dummyErrorImpl.*

      given Runtime[FrontendEnv] = runtime

      val checkMe = services.http.get[Option[User]](models.users.Routes.me).orDie

      def app = div(
        child <-- Routes
          .firstOf(
            Route(routing.base / models.users.Routes.login / endOfSegments, () => Login(checkMe)),
            Route(
              (routing.base / models.bff.Routes.gamePlayingRoot) ? models.bff.Routes.gameIdParam,
              (_, gameId) => DuringGameMainPage(checkMe, gameId)
            ),
            Route(routing.base, () => MainPage(checkMe))
          )
          .map(
            _.getOrElse(
              IllustratedMessage(
                _.name      := IllustratedMessageType.Tent,
                _.titleText := "You seem to be lost.",
                _.slots.subtitle := Text(
                  "Don't worry, you can simply click ",
                  components.router.Link(root)("here"),
                  "."
                )
              )
            )
          )
      )

      render(dom.document.getElementById("root"), app)
    case Failure(exception) =>
      exception.printStackTrace()
      render(
        dom.document.getElementById("root"),
        div(
          Title.h1("Battle for Flatland"),
          MessageStrip(
            _.design := MessageStripDesign.Critical,
            "Something went terribly wrong launching the Battle for Flatland application. Please contact an administrator."
          )
        )
      )
  }(scala.concurrent.ExecutionContext.global)
}
