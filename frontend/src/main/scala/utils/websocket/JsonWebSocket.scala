package utils.websocket

import com.raquo.laminar.api.A.*
import com.raquo.airstream.ownership.Owner
import io.circe.parser.decode
import io.circe.{Decoder, Encoder}
import org.scalajs.dom
import org.scalajs.dom.{Event, WebSocket}
import urldsl.language.QueryParameters.dummyErrorImpl.*
import urldsl.language.*
import zio.*
import utils.laminarzio.onMountZIO
import utils.laminarzio.onMountUnmountCallbackWithStateZIO
import com.raquo.laminar.nodes.ReactiveElement
import services.FrontendEnv

/** Prepares a WebSocket to connect to the specified url. The connection actually occurs when you
  * run the `open` method.
  *
  * Messages coming from the server can be retrieved using the `$in`
  * [[com.raquo.airstream.eventstream.EventStream]] and sending messages to the server can be done
  * by writing to the `outWriter` [[com.raquo.airstream.core.Observer]]
  */
final class JsonWebSocket[In, Out, P, Q] private (
    pathWithQueryParams: PathSegmentWithQueryParams[P, ?, Q, ?],
    p: P,
    q: Q,
    host: String
)(using
    decoder: Decoder[In],
    encoder: Encoder[Out]
) {

  private def url: String =
    "ws://" + host + "/ws/" + pathWithQueryParams.createUrlString(p, q)

  private lazy val socket = new WebSocket(url)

  private val inBus: EventBus[In]           = new EventBus
  private val outBus: EventBus[Out]         = new EventBus
  private val closeBus: EventBus[Unit]      = new EventBus
  private val errorBus: EventBus[dom.Event] = new EventBus
  private val openBus: EventBus[dom.Event]  = new EventBus

  private def openWebSocketConnection(using owner: Owner) = ZIO.succeed {
    val webSocket = socket
    webSocket.onmessage = (event: dom.MessageEvent) =>
      decode[In](event.data.asInstanceOf[String]) match {
        case Right(in) => inBus.writer.onNext(in)
        case Left(error) =>
          dom.console.log("data", event.data)
          dom.console.error(error)
      }
    outBus.events.map(encoder(_).noSpaces).foreach(webSocket.send)
    webSocket.onopen = (event: Event) => openBus.writer.onNext(event)
    webSocket.onerror = (event: Event) => {
      if (scala.scalajs.LinkingInfo.developmentMode) {
        dom.console.error(event)
      }
      errorBus.writer.onNext(event)
    }
    webSocket.onclose = (_: Event) => closeBus.writer.onNext(())
  }

  def open(using Owner): ZIO[Any, Nothing, Unit] = openWebSocketConnection

  def modifier[El <: ReactiveElement.Base](using Runtime[FrontendEnv]) =
    onMountUnmountCallbackWithStateZIO(
      ctx => open(using ctx.owner),
      (_, _) => ZIO.succeed(socket.close())
    )

  def close(): Unit = {
    socket.close()
    closeBus.writer.onNext(())
  }

  val inEvents: EventStream[In]       = inBus.events
  val outWriter: WriteBus[Out]        = outBus.writer
  val closedSignal: Signal[Boolean]   = closeBus.events.mapTo(true).startWith(false)
  val errorEvents: EventStream[Event] = errorBus.events
  val openEvents: EventStream[Event]  = openBus.events

}

object JsonWebSocket {

  def apply[In, Out](path: PathSegment[Unit, ?], host: String = dom.document.location.host)(using
      Decoder[In],
      Encoder[Out]
  ): JsonWebSocket[In, Out, Unit, Unit] = new JsonWebSocket(path ? ignore, (), (), host)

  def apply[In, Out, Q](
      path: PathSegment[Unit, ?],
      query: QueryParameters[Q, ?],
      q: Q,
      host: String
  )(using Decoder[In], Encoder[Out]): JsonWebSocket[In, Out, Unit, Q] =
    new JsonWebSocket(path ? query, (), q, host)

  def apply[In, Out, Q](
      path: PathSegment[Unit, ?],
      query: QueryParameters[Q, ?],
      q: Q
  )(using Decoder[In], Encoder[Out]): JsonWebSocket[In, Out, Unit, Q] =
    apply(path, query, q, dom.document.location.host)

}
