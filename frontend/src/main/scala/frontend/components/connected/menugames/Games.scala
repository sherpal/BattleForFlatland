package frontend.components.connected.menugames

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import errors.ErrorADT
import frontend.components.{Component, ModalWindow}
import models.bff.outofgame.MenuGame
import org.scalajs.dom.raw.NonDocumentTypeChildNode
import org.scalajs.dom.{html, raw}
import programs.frontend.games
import services.http.FHttpClient
import services.routing.FRouting
import urldsl.language.PathSegment.dummyErrorImpl._
import utils.laminarzio.Implicits._
import utils.websocket.JsonWebSocket

final class Games private () extends Component[html.Div] {

  final val layer = FHttpClient.live ++ zio.clock.Clock.live ++ FRouting.live

  final val socket = JsonWebSocket[String, String](root / "game-menu-room")

  //final val $games = gamesOrErrors$.collect { case Right(gameList) => gameList }

  final val $games = socket.$in.filter(_.nonEmpty)
    .flatMap(
      _ => EventStream.fromZIOEffect[Either[ErrorADT, List[MenuGame]]](games.downloadGames.either.provideLayer(layer))
    )
    .collect { case Right(gameList) => gameList }

  val showNewGameBus: EventBus[Boolean] = new EventBus
  val showWriter: Observer[Unit]        = showNewGameBus.writer.contramap[Unit](_ => true)
  val closeWriter: Observer[Unit]       = showNewGameBus.writer.contramap[Unit](_ => false)
  val showNewGame$ : Signal[ReactiveHtmlElement[html.Div]] =
    showNewGameBus.events.startWith(false).map {
      if (_) ModalWindow(NewGame(closeWriter), closeWriter)
      else div()
    }

  val element: ReactiveHtmlElement[html.Div] = div(
    child <-- showNewGame$,
    DisplayGames($games, showWriter),
    onMountCallback { ctx =>
      socket.open()(ctx.owner)
      zio.Runtime.default.unsafeRunToFuture(games.amIAmPlayingSomewhere.provideLayer(layer))
    },
    onUnmountCallback(_ => socket.close())
  )

}

object Games {
  def apply() = new Games
}
