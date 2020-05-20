package frontend.components.connected.menugames

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import errors.ErrorADT
import errors.ErrorADT.{ErrorOr, IncorrectGamePassword}
import frontend.components.forms.SimpleForm
import frontend.components.utils.tailwind._
import frontend.components.utils.tailwind.forms._
import frontend.components.utils.tailwind.modal._
import frontend.components.{Component, ModalWindow}
import models.bff.outofgame.MenuGame
import models.common.PasswordWrapper
import models.syntax.Pointed
import models.validators.FieldsValidator
import org.scalajs.dom.html
import programs.frontend.games._
import services.http.FHttpClient
import services.routing.FRouting
import zio.UIO

final class JoinGameModal private (game: MenuGame, closeWriter: ModalWindow.CloseWriter)(
    implicit pwPointed: Pointed[PasswordWrapper]
) extends Component[html.Element]
    with SimpleForm[PasswordWrapper, ErrorOr[Int]] {

  private val layer = FHttpClient.live ++ FRouting.live

  val passwordChanger: Observer[String] = makeDataChanger(password => _.copy(submittedPassword = Some(password)))

  /** Error displays are washed out when password input receive focus.
    * This is done by poking this bus.
    */
  val passwordTouchedBus: EventBus[Unit]  = new EventBus
  val $passwordTouches: EventStream[Unit] = passwordTouchedBus.events

  val $maybePasswordErrors: EventStream[Option[ErrorADT]] = EventStream.merge(
    $passwordTouches.mapTo(None),
    $submitEvents.map(_.swap.toOption)
  )

  val element: ReactiveHtmlElement[html.Element] = aside(
    modalContainer,
    h1(
      className := s"text-xl text-$primaryColour-$primaryColourDark",
      s"Join game ${game.gameName}"
    ),
    form(
      submit,
      game.maybeHashedPassword match {
        case Some(_) =>
          fieldSet(
            div(
              formGroup,
              formLabel("Password"),
              formInput(
                "password",
                placeholder := "Enter game password",
                inContext(elem => onChange.mapTo(elem.ref.value) --> passwordChanger),
                onFocus.mapTo(()) --> passwordTouchedBus,
                className <-- $maybePasswordErrors.map(_.isDefined).map(if (_) "border-red-500" else ""),
                focus <-- $submitEvents.mapTo(false)
              )
            )
          )
        case None =>
          fieldSet(
            p(
              formPrimaryTextColour,
              className := "py-5",
              "Confirm join game?"
            )
          )
      },
      div(
        p(
          className := "text-red-600",
          child.text <-- $maybePasswordErrors.map {
            case Some(IncorrectGamePassword) => "Invalid password"
            case Some(error)                 => s"Unhandled error ($error)."
            case _                           => ""
          }
        )
      ),
      div(
        formGroup,
        div(
          input(
            `type` := "submit",
            value := "Join game",
            btn,
            primaryButton,
            disabled <-- $isSubmitting
          ),
          span(
            btn,
            secondaryButton,
            "Cancel",
            onClick.stopPropagation.mapTo(()) --> closeWriter
          )
        )
      )
    )
  )
  val initialData: PasswordWrapper                          = pwPointed.unit
  val validator: FieldsValidator[PasswordWrapper, ErrorADT] = FieldsValidator.allowAllValidator

  def submitProgram(formData: PasswordWrapper): UIO[ErrorOr[Int]] =
    joinGameProgram(game, formData).refineOrDie(ErrorADT.onlyErrorADT).either.provideLayer(layer)

}

object JoinGameModal {
  def apply(game: MenuGame, closeWriter: ModalWindow.CloseWriter)(
      implicit pwPointed: Pointed[PasswordWrapper]
  ) = new JoinGameModal(game, closeWriter)
}
