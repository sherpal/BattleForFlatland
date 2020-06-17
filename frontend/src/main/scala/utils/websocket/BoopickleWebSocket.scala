package utils.websocket

import java.nio.ByteBuffer

import urldsl.language.{PathSegment, PathSegmentWithQueryParams, QueryParameters}
import boopickle.Default._
import com.raquo.airstream.eventbus.{EventBus, WriteBus}
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.Owner
import org.scalajs.dom
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{Event, WebSocket}
import slinky.web.html.value
import zio.{CancelableFuture, UIO, ZIO}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}
import scala.util.{Failure, Success}

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

  private lazy val socket = new WebSocket(url)

  private val inBus: EventBus[In]           = new EventBus
  private val outBus: EventBus[Out]         = new EventBus
  private val closeBus: EventBus[Unit]      = new EventBus
  private val errorBus: EventBus[dom.Event] = new EventBus
  private val openBus: EventBus[dom.Event]  = new EventBus

  /**
    * Data currently comes in as blob, which need to be translated into array buffer. However, this is asynchronous and
    * we can't be sure that the order of message will be preserved. That implies that we need to do the following
    * nonsense.
    */
  private var lastMessageId: Long                      = 0L
  private var lastMessageIdWentThrough: Long           = 0L
  private val waitingMessages: mutable.Set[(In, Long)] = mutable.Set.empty

  private def sendThroughMessages(nextIn: In, nextId: Long): Unit =
    if (nextId == lastMessageIdWentThrough + 1) {
      lastMessageIdWentThrough += 1
      inBus.writer.onNext(nextIn)
      waitingMessages.find(_._2 == lastMessageIdWentThrough + 1).foreach {
        case (in, id) =>
          waitingMessages.remove((in, id))
          sendThroughMessages(in, id)
      }
    } else {
      waitingMessages += ((nextIn, nextId))
    }

  private def openWebSocketConnection(implicit owner: Owner) =
    for {
      webSocket <- UIO(socket)
      _ <- UIO {
        webSocket.onmessage = (event: MessageEvent) => {
          val blob = event.data.asInstanceOf[typings.std.Blob]
          val messageId = {
            lastMessageId += 1
            lastMessageId
          }

          (blob.arrayBuffer().toFuture zip Future.successful(messageId)).onComplete {
            case Failure(exception) =>
              throw exception
            case Success((arrayBuffer, messageId)) =>
              val in = {
                val array = new Uint8Array(arrayBuffer)
                Unpickle
                  .apply[In]
                  .fromBytes(ByteBuffer.wrap(array.toArray.map(_.asInstanceOf[Byte])))
              }

              sendThroughMessages(in, messageId)
          }
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
