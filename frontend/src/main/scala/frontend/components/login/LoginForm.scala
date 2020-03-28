package frontend.components.login

import com.raquo.laminar.nodes.ReactiveHtmlElement
import errors.ErrorADT
import frontend.components.Component
import frontend.components.forms.SimpleForm
import models.users.LoginUser
import models.validators.FieldsValidator
import org.scalajs.dom.html
import org.scalajs.dom.html.Form
import com.raquo.laminar.api.L._
import errors.ErrorADT.ErrorOr
import programs.frontend.login.login
import services.http.FrontendHttpClient
import zio.{UIO, ZIO}

final class LoginForm private () extends Component[html.Form] with SimpleForm[LoginUser, ErrorOr[Int]] {

  val initialData: LoginUser                          = LoginUser.empty
  val validator: FieldsValidator[LoginUser, ErrorADT] = LoginUser.validator

  val nameChanger: Observer[String]     = makeDataChanger((name: String) => _.copy(userName = name))
  val passwordChanger: Observer[String] = makeDataChanger((pw: String) => _.copy(password   = pw))

  def submitProgram(loginUser: LoginUser): UIO[ErrorOr[Int]] =
    login(loginUser).provideLayer(FrontendHttpClient.live)

  implicit val element: ReactiveHtmlElement[Form] = form(
    submit,
    fieldSet(
      label("Name"),
      input(
        `type` := "text",
        inContext(elem => onChange.mapTo(elem.ref.value) --> nameChanger)
      )
    ),
    fieldSet(
      label("Password"),
      input(`type` := "password", inContext(elem => onChange.mapTo(elem.ref.value) --> passwordChanger))
    ),
    input(`type` := "submit", value := "login"),
    child <-- $submitEvents.map {
      case Right(_)    => "connected"
      case Left(error) => error.toString
    }
  )

  $formData.foreach(println)

}

object LoginForm {
  def apply(): LoginForm = new LoginForm
}
