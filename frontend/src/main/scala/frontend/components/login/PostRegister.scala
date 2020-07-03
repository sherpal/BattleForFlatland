package frontend.components.login

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import frontend.components.utils.PrimaryLink
import models.users.RouteDefinitions
import org.scalajs.dom.html
import services.http._
import services.routing.{moveTo, FRouting, Routing}
import zio.ZIO

final class PostRegister private (userName: String) extends Component[html.Element] {

  val autoConfirmRegistration: ZIO[Routing with HttpClient, Throwable, Unit] = for {
    _ <- ZIO.effectTotal { println("Asking registration key") }
    registrationKey <- post[String, String](
      RouteDefinitions.registrationKeyFromNameRoute,
      RouteDefinitions.userNameParam
    )(userName)
    _ <- moveTo(RouteDefinitions.confirmRoute)(registrationKey)
  } yield ()

  val element: ReactiveHtmlElement[html.Element] = section(
    p(s"Thank you, $userName, for registering to Battle For Flatland!"),
    p("You should soon receive an email with a confirmation link to follow in order to confirm you registration."),
    PrimaryLink(RouteDefinitions.loginRoute)("Login"),
    onMountCallback { _ =>
      zio.Runtime.default.unsafeRunToFuture(
        autoConfirmRegistration.provideLayer(FHttpClient.live ++ FRouting.live)
      )
    }
  )
}

object PostRegister {
  def apply(userName: String): PostRegister = new PostRegister(userName)
}
