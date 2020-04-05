package frontend.components.connected.menugames

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.utils.ToggleButton
import frontend.components.utils.tailwind._
import frontend.components.utils.tailwind.forms._
import frontend.components.{Component, ModalWindow}
import org.scalajs.dom.html

final class NewGame private (closeWriter: ModalWindow.CloseWriter) extends Component[html.Div] { //} with SimpleForm[MenuGame, Unit] {

  val withPasswordBus: EventBus[Boolean] = new EventBus

  val $withPassword: Signal[Boolean] = withPasswordBus.events.startWith(false)

  val element: ReactiveHtmlElement[html.Div] = div(
    className := "bg-white rounded-lg border-gray-200 border-2 w-auto whitespace-no-wrap",
    pad(5),
    h1(className := s"text-lg text-$primaryColour-$primaryColourDark", "New game"),
    form(
      fieldSet(
        div(
          formGroup,
          formLabel("Game name"),
          formInput("text", placeholder := "Choose a game name")
        ),
        div(
          "Private game ",
          ToggleButton(withPasswordBus.writer)
        ),
        div(
          className <-- $withPassword.map(if (_) "flex" else "hidden"),
          div(
            formGroup,
            formLabel("Game password"),
            formInput("password", placeholder := "Password for the game")
          )
        )
      ),
      div(
        formGroup,
        div(className := "md:w-1/3"),
        div(
          className := "md:w-2/3 justify-between",
          input(
            `type` := "submit",
            "Create game",
            btn,
            primaryButton
          ),
          span(
            secondaryButton,
            "Cancel",
            onClick.mapTo(()) --> closeWriter
          )
        )
      )
    )
  )

}

object NewGame {
  def apply(closeWriter: Observer[Unit]) = new NewGame(closeWriter)
}
