package frontend.components.connected.ingame

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import errors.ErrorADT
import frontend.components.Component
import models.users.User
import org.scalajs.dom.html
import programs.frontend.login.me
import services.http.FHttpClient
import services.logging.FLogging
import services.routing.FRouting
import utils.laminarzio.Implicits._

final class GamePlayingContainer private (gameId: String, token: String) extends Component[html.Element] {

  private val layer = FHttpClient.live ++ FLogging.live ++ FRouting.live

  val $me: EventStream[Either[ErrorADT, User]] = EventStream.fromZIOEffect(me.either.provideLayer(layer))

  val element: ReactiveHtmlElement[html.Element] = div(
    child <-- $me.collect { case Right(user) => user }.map(GamePlaying(gameId, _, token))
  )

}

object GamePlayingContainer {
  def apply(gameId: String, token: String) = new GamePlayingContainer(gameId, token)
}
