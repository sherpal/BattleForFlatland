package utils.websocket

import com.raquo.airstream.eventbus.{EventBus, WriteBus}
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.Owner
import io.circe.parser.decode
import io.circe.{Decoder, Encoder}
import org.scalajs.dom
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{Event, WebSocket}
import zio.{UIO, ZIO}

/**
  * Prepares a WebSocket to connect to the specified url.
  * The connection actually occurs when you run the `open` method.
  *
  * Messages coming from the server can be retrieved using the `$in` [[com.raquo.airstream.eventstream.EventStream]]
  * and sending messages to the server can be done by writing to the `outWriter` [[com.raquo.airstream.core.Observer]]
  */
final class JsonWebSocket[In, Out](
    url: String
)(
    implicit decoder: Decoder[In],
    encoder: Encoder[Out]
) {

  private val socket = ZIO.effect(new WebSocket(url))

  private val inBus: EventBus[In]   = new EventBus[In]
  private val outBus: EventBus[Out] = new EventBus[Out]

  private def openWebSocketConnection(implicit owner: Owner) =
    for {
      webSocket <- socket
      _ <- UIO {
        webSocket.onmessage = (event: MessageEvent) => {
          decode[In](event.data.asInstanceOf[String]) match {
            case Right(in) => inBus.writer.onNext(in)
            case Left(error) =>
              dom.console.log("data", event.data)
              dom.console.error(error)
          }
        }
      }
      _ <- UIO {
        outBus.events.map(encoder.apply).map(_.noSpaces).foreach(webSocket.send)
      }
      _ <- UIO {
        webSocket.onerror = (event: Event) => dom.console.error(event)
      }
    } yield ()

  def open()(implicit owner: Owner): Unit =
    zio.Runtime.default.unsafeRunToFuture(openWebSocketConnection)

  val $in: EventStream[In]     = inBus.events
  val outWriter: WriteBus[Out] = outBus.writer

}
