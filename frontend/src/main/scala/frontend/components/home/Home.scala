package frontend.components.home

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import models.users.User
import org.scalajs.dom.html
import utils.laminarzio.Implicits._
import programs.frontend.login.{amISuperUser, me}
import services.http.FrontendHttpClient.{live => httpLive}

final class Home private () extends Component[html.Div] {

  val $user: EventStream[User] = EventStream.fromZIOEffect(me.provideLayer(httpLive))
  val $amISuperUper: EventStream[Boolean] =
    EventStream.fromZIOEffect(amISuperUser.provideLayer(httpLive))

  val element: ReactiveHtmlElement[html.Div] = div(
    h1("Battle for Flatland"),
    p(s"Hello, ", child <-- $user.map(_.userName)),
    p(
      child <-- $amISuperUper.map {
        if (_) "You are a SuperUser."
        else "You are not a SuperUser."
      }
    )
  )
}

object Home {
  def apply() = new Home
}
