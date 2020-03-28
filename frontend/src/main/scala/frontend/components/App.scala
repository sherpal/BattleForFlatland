package frontend.components

import assets.ScalaLogo
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.home.Home
import frontend.components.login.{ConfirmRegistration, Login, PostRegister, Register}
import frontend.router.RouteDefinitions._
import frontend.router.{Route, Routes}
import org.scalajs.dom
import org.scalajs.dom.html.Div

final class App private () extends Component[dom.html.Div] {

  val element: ReactiveHtmlElement[Div] = div(
    img(src := ScalaLogo),
    child <-- Routes
      .firstOf(
        Route(loginRoute, () => Login()),
        Route(registerRoute, () => Register()),
        Route(postRegisterRoute, (_: Unit, userName: String) => PostRegister(userName)),
        Route(confirmRoute, (_: Unit, key: String) => ConfirmRegistration(key)),
        Route(homeRoute, () => Home())
      )
      .map {
        case Some(elem) => elem
        case None       => div("uh oh") // todo: 404
      }
  )

}

object App {
  def apply(): App = new App
}
