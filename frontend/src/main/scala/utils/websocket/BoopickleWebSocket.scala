package utils.websocket

import java.nio.ByteBuffer

import boopickle.Default._
import com.raquo.airstream.eventbus.{EventBus, WriteBus}
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.Owner
import org.scalajs.dom
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{Event, WebSocket}
import urldsl.language.{PathSegment, PathSegmentWithQueryParams, QueryParameters}
import zio.{CancelableFuture, UIO, ZIO}

import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}

// todo: make a WebSocketEndpoint above the Boopickle and the Circe one.
final class BoopickleWebSocket[In, Out, P, Q] private (
    pathWithQueryParams: PathSegmentWithQueryParams[P, _, Q, _],
    p: P,
    q: Q,
    host: String
)(
    implicit inPickler: Pickler[In],
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

  private def openWebSocketConnection(implicit owner: Owner) =
    for {
      webSocket <- UIO(socket)
      _ <- UIO {
        webSocket.onmessage = (event: MessageEvent) => {
          val arrayBuffer = event.data.asInstanceOf[ArrayBuffer]
          val array       = new Uint8Array(arrayBuffer)
          inBus.writer.onNext(
            Unpickle
              .apply[In]
              .fromBytes(ByteBuffer.wrap(array.toArray.map(_.asInstanceOf[Byte])))
          )
        }
      }
      _ <- UIO {
        outBus.events
          .map(Pickle.intoBytes[Out])
          .map { byteBuffer =>
            val array = new Array[Byte](byteBuffer.remaining())
            byteBuffer.get(array)
            val arrayBuffer = new ArrayBuffer(array.length)
            val view        = new Uint8Array(arrayBuffer)
            for (idx <- array.indices) {
              view(idx) = array(idx)
            }
            arrayBuffer
          }
          .foreach(webSocket.send)
      }
      _ <- UIO { webSocket.onopen = (event: Event) => openBus.writer.onNext(event) }
      _ <- UIO {
        webSocket.onerror = (event: Event) => {
          if (scala.scalajs.LinkingInfo.developmentMode) {
            dom.console.error(event)
          }
          errorBus.writer.onNext(event)
        }
      }
      _ <- ZIO.effectTotal {
        webSocket.onclose = (_: Event) => {
          closeBus.writer.onNext(())
        }
      }
    } yield ()

  def open()(implicit owner: Owner): CancelableFuture[Unit] =
    zio.Runtime.default.unsafeRunToFuture(openWebSocketConnection)

  def close(): Unit = {
    socket.close()
    closeBus.writer.onNext(())
  }

  val $in: EventStream[In]       = inBus.events
  val outWriter: WriteBus[Out]   = outBus.writer
  val $closed: EventStream[Unit] = closeBus.events
  val $error: EventStream[Event] = errorBus.events
  val $open: EventStream[Event]  = openBus.events
}

object BoopickleWebSocket {

  import urldsl.language.QueryParameters.dummyErrorImpl._

  def apply[In, Out](path: PathSegment[Unit, _], host: String = dom.document.location.host)(
      implicit inPickler: Pickler[In],
      outPickler: Pickler[Out]
  ): BoopickleWebSocket[In, Out, Unit, Unit] = new BoopickleWebSocket(path ? empty, (), (), host)

  def apply[In, Out, Q](
      path: PathSegment[Unit, _],
      query: QueryParameters[Q, _],
      q: Q,
      host: String
  )(
      implicit inPickler: Pickler[In],
      outPickler: Pickler[Out]
  ): BoopickleWebSocket[In, Out, Unit, Q] = new BoopickleWebSocket(path ? query, (), q, host)

  def apply[In, Out, Q](
      path: PathSegment[Unit, _],
      query: QueryParameters[Q, _],
      q: Q
  )(
      implicit inPickler: Pickler[In],
      outPickler: Pickler[Out]
  ): BoopickleWebSocket[In, Out, Unit, Q] = apply(path, query, q, dom.document.location.host)

}
