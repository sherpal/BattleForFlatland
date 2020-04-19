package frontend.components.connected.ingame

import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.LifecycleComponent
import models.bff.ingame.InGameWSProtocol
import models.users.User
import org.scalajs.dom.html
import utils.websocket.JsonWebSocket
import models.bff.Routes._
import com.raquo.laminar.api.L._
import io.circe.syntax._

final class GamePlaying private (gameId: String, user: User, token: String) extends LifecycleComponent[html.Div] {

  final val gameSocket = JsonWebSocket[InGameWSProtocol, InGameWSProtocol, (String, String)](
    joinGameServer,
    userIdAndTokenParams,
    (user.userId, token),
    host = "localhost:22222" // todo: change this!
  )

  val elem: ReactiveHtmlElement[html.Div] = div(
    pre(
      child.text <-- gameSocket.$in.fold(List[InGameWSProtocol]())(_ :+ _)
        .map(_.map(_.asJson.spaces2))
        .map(_.mkString("\n"))
    )
  )

  override def componentDidMount(): Unit = {
    println(s"Game id: $gameId.")
    gameSocket.open()(elem)
  }

}

object GamePlaying {
  def apply(gameId: String, user: User, token: String) = new GamePlaying(gameId, user, token)
}
