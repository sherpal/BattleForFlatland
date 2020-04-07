package frontend.components.connected.menugames

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.{ReactiveChildNode, ReactiveHtmlElement}
import errors.ErrorADT
import frontend.components.{LifecycleComponent, ModalWindow}
import models.bff.outofgame.MenuGame
import org.scalajs.dom.raw.NonDocumentTypeChildNode
import org.scalajs.dom.{html, raw}
import programs.frontend.games
import services.http.FrontendHttpClient
import utils.laminarzio.Implicits._
import utils.websocket.JsonWebSocket

final class Games private () extends LifecycleComponent[html.Div] {

  final val layer = FrontendHttpClient.live ++ zio.clock.Clock.live

  final val socket = new JsonWebSocket[String, String]("ws://localhost:8080/ws/game-menu-room")

  val (cancelDLGames, gamesOrErrors$) = EventStream.fromZStream(games.loadGames.provideLayer(layer))

  override def componentWillUnmount(): Unit = cancelDLGames.cancel()

  //final val $games = gamesOrErrors$.collect { case Right(gameList) => gameList }

  final val $games = socket.$in.filter(_.nonEmpty)
    .map(println)
    .flatMap(
      _ => EventStream.fromZIOEffect[Either[ErrorADT, List[MenuGame]]](games.downloadGames.either.provideLayer(layer))
    )
    .collect { case Right(gameList) => gameList }

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

  override def componentDidMount(): Unit = socket.open()(elem)
}

object Games {
  def apply() = new Games
}
