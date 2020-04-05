package frontend.components.connected.menugames

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.{ReactiveChildNode, ReactiveHtmlElement}
import frontend.components.{LifecycleComponent, ModalWindow}
import org.scalajs.dom.raw.NonDocumentTypeChildNode
import org.scalajs.dom.{html, raw}
import programs.frontend.games
import services.http.FrontendHttpClient
import utils.laminarzio.Implicits._

final class Games private () extends LifecycleComponent[html.Div] {

  final val layer = FrontendHttpClient.live ++ zio.clock.Clock.live

  val (cancelDLGames, gamesOrErrors$) = EventStream.fromZStream(games.loadGames.provideLayer(layer))

  override def componentWillUnmount(): Unit = cancelDLGames.cancel()

  final val $games = gamesOrErrors$.collect { case Right(gameList) => gameList }

  val showNewGameBus: EventBus[Boolean] = new EventBus
  val showWriter: Observer[Unit]        = showNewGameBus.writer.contramap[Unit](_ => true)
  val closeWriter: Observer[Unit]       = showNewGameBus.writer.contramap[Unit](_ => false)
  val showNewGame$ : Signal[ReactiveChildNode[raw.Node with NonDocumentTypeChildNode]] =
    showNewGameBus.events.startWith(false).map {
      if (_) ModalWindow(NewGame(closeWriter), closeWriter)
      else emptyNode
    }

  val elem: ReactiveHtmlElement[html.Div] = div(
    child <-- showNewGame$,
    DisplayGames($games, showWriter)
  )
}

object Games {
  def apply() = new Games
}
