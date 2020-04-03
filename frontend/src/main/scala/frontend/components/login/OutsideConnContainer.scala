package frontend.components.login

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import frontend.components.utils.bootstrap.ModalWindow
import frontend.router.{Route, Routes}
import models.users.RouteDefinitions._
import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.html.Div
import typings.popperjsCore.{mod => Popper}

final class OutsideConnContainer private (title: String) extends Component[html.Div] {

  val element: ReactiveHtmlElement[Div] =
    div(
      className := "outside-conn",
      HeaderBar(title),
      div(className := "empty1"),
      child <-- Routes
        .firstOf(
          Route(loginRoute, () => Login()),
          Route(registerRoute, () => Register()),
          Route(postRegisterRoute, (_: Unit, userName: String) => PostRegister(userName)),
          Route(confirmRoute, (_: Unit, key: String) => ConfirmRegistration(key))
        )
        .map {
          case Some(elem) => elem
          case None       => div("uh oh") // todo: 404
        },
      div(
        className := "empty2",
        button(
          "try me",
          onClick.mapTo(
            (
              "Modal !",
              p("this is the body of the modal"),
              "finished!",
              (_: ReactiveHtmlElement[html.Button]) => onClick.mapTo(()) --> (_ => println("yeah"))
            )
          ) --> ModalWindow.showModalBusWriter
        )
      )
    )

}

object OutsideConnContainer {
  def apply(title: String) = new OutsideConnContainer(title)
}
