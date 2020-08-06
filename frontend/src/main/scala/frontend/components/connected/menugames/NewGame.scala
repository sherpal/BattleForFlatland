package frontend.components.connected.menugames

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import errors.ErrorADT
import errors.ErrorADT.ErrorOr
import frontend.components.forms.SimpleForm
import frontend.components.utils.ToggleButton
import frontend.components.utils.tailwind._
import frontend.components.utils.tailwind.forms._
import frontend.components.utils.tailwind.modal._
import frontend.components.{Component, ModalWindow}
import models.bff.outofgame.MenuGame
import models.common.PasswordWrapper
import models.syntax.{Pointed, Validated}
import models.validators.FieldsValidator
import org.scalajs.dom.html
import programs.frontend.games._
import services.http.FHttpClient
import services.routing.FRouting
import zio.UIO

final class NewGame private (closeWriter: ModalWindow.CloseWriter)(
    implicit
    menuGamePointed: Pointed[MenuGame],
    validated: Validated[MenuGame, ErrorADT]
) extends Component[html.Div]
    with SimpleForm[MenuGame, ErrorOr[Int]] {

  val initialData: MenuGame                          = menuGamePointed.unit
  val validator: FieldsValidator[MenuGame, ErrorADT] = validated.fieldsValidator

  private val layer = FHttpClient.live ++ FRouting.live

  val gameNameChanger: Observer[String] = makeDataChanger[String](gameName => _.copy(gameName = gameName))
  val passwordChanger: Observer[Option[String]] = makeDataChanger(
    maybePW => _.copy(maybeHashedPassword = maybePW)
  )

  val $withPassword: Signal[Boolean] = $formData.map(_.maybeHashedPassword.isDefined)

  val createdBus: EventBus[Unit] = new EventBus

  val element: ReactiveHtmlElement[html.Div] = div(
    modalContainer,
    width := "800px",
    h1(className := s"text-lg text-$primaryColour-$primaryColourDark", "New game"),
    form(
      submit,
      fieldSet(
        div(
          formGroup,
          formLabel("Game name"),
          formInput(
            "text",
            placeholder := "Choose a game name",
            inContext(elem => onChange.mapTo(elem.ref.value) --> gameNameChanger),
            onMountFocus
          )
        ),
        div(
          className := "flex",
          label("Private game", className := "font-bold text-gray-500 pr-4 md:w-1/3"),
          span(
            className := "md:w-2/3",
            ToggleButton(passwordChanger.contramap[Boolean](if (_) Some("") else None))
          )
        ),
        div(
          child <-- $withPassword.map(
            if (_)
              div(
                formGroup,
                formLabel("Game password"),
                formInput(
                  "password",
                  placeholder := "Password for the game",
                  inContext(elem => onChange.mapTo(Some(elem.ref.value)) --> passwordChanger)
                )
              )
            else emptyNode
          )
        )
      ),
      div(
        p(
          className := "text-red-600",
          pre(
            child.text <-- $submitEvents.collect { case Left(error) => error }
              .map(_.json)
              .map(_.spaces2)
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
            primaryButton,
            disabled <-- $isSubmitting
          ),
          span(
            btn,
            secondaryButton,
            "Cancel",
            onClick.mapTo(()) --> closeWriter
          )
        )
      )
    ),
    onMountCallback(_ => createdBus.writer.onNext(()))
  )

  def submitProgram(formData: MenuGame): UIO[ErrorOr[Int]] =
    (for {
      gameId <- createNewGame(formData)
      gameJoined <- joinGameProgram(formData.copy(gameId = gameId), PasswordWrapper(formData.maybeHashedPassword))
      _ <- UIO(closeWriter.onNext(()))
    } yield gameJoined)
      .refineOrDie(ErrorADT.onlyErrorADT)
      .either
      .provideLayer(layer)

}

object NewGame {
  def apply(closeWriter: Observer[Unit]) = new NewGame(closeWriter)
}
