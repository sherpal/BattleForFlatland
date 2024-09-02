package utils.websocket

import java.nio.ByteBuffer

import boopickle.Default.*
import com.raquo.laminar.api.A.*
import com.raquo.airstream.ownership.Owner
import org.scalajs.dom
import org.scalajs.dom.MessageEvent
import org.scalajs.dom.{Event, WebSocket}
import urldsl.language.{PathSegment, PathSegmentWithQueryParams, QueryParameters}
import zio.{CancelableFuture, UIO, ZIO}

import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}
import com.raquo.laminar.nodes.ReactiveElement
import services.FrontendEnv
import utils.laminarzio.onMountUnmountCallbackWithStateZIO

// todo: make a WebSocketEndpoint above the Boopickle and the Circe one.
final class BoopickleWebSocket[In, Out, P, Q] private (
    pathWithQueryParams: PathSegmentWithQueryParams[P, ?, Q, ?],
    p: P,
    q: Q,
    host: String
)(using
    inPickler: Pickler[In],
    outPickler: Pickler[Out]
) {
  private def url: String = "ws://" + host + "/ws/" + pathWithQueryParams.createUrlString(p, q)

  private lazy val socket = {
    val s = new WebSocket(url)
    s.binaryType = "arraybuffer"
    s
  }

  private val inBus: EventBus[In]           = new EventBus
  private val outBus: EventBus[Out]         = new EventBus
  private val closeBus: EventBus[Unit]      = new EventBus
  private val errorBus: EventBus[dom.Event] = new EventBus
  private val openBus: EventBus[dom.Event]  = new EventBus

  private def openWebSocketConnection(using Owner) = ZIO.succeed {
    val webSocket = socket
    val unpickler = Unpickle.apply[In]
    webSocket.onmessage = (event: MessageEvent) => {
      val arrayBuffer = event.data.asInstanceOf[ArrayBuffer]
      val array       = new Uint8Array(arrayBuffer)
      val in = unpickler.fromBytes(ByteBuffer.wrap(array.toArray.map(_.asInstanceOf[Byte])))
      inBus.writer.onNext(in)
    }

    outBus.events
      .map(Pickle.intoBytes[Out])
      .map { byteBuffer =>
        val array = new Array[Byte](byteBuffer.remaining())
        byteBuffer.get(array)
        val arrayBuffer = new ArrayBuffer(array.length)
        val view        = new Uint8Array(arrayBuffer)
        for (idx <- array.indices)
          view(idx) = array(idx)
        arrayBuffer
      }
      .foreach(webSocket.send)

    webSocket.onopen = (event: Event) => openBus.writer.onNext(event)
    webSocket.onclose = (_: Event) => closeBus.writer.onNext(())
  }

  def open(using Owner): ZIO[Any, Nothing, Unit] =
    openWebSocketConnection

  def modifier[El <: ReactiveElement.Base](using zio.Runtime[FrontendEnv]) =
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

object BoopickleWebSocket {

  import urldsl.language.QueryParameters.dummyErrorImpl._

  def apply[In, Out](path: PathSegment[Unit, ?], host: String = dom.document.location.host)(using
      Pickler[In],
      Pickler[Out]
  ): BoopickleWebSocket[In, Out, Unit, Unit] = new BoopickleWebSocket(path ? ignore, (), (), host)

  def apply[In, Out, Q](
      path: PathSegment[Unit, ?],
      query: QueryParameters[Q, ?],
      q: Q,
      host: String
  )(using Pickler[In], Pickler[Out]): BoopickleWebSocket[In, Out, Unit, Q] =
    new BoopickleWebSocket(path ? query, (), q, host)

  def apply[In, Out, Q](
      path: PathSegment[Unit, ?],
      query: QueryParameters[Q, ?],
      q: Q
  )(using Pickler[In], Pickler[Out]): BoopickleWebSocket[In, Out, Unit, Q] =
    apply(path, query, q, dom.document.location.host)

}
