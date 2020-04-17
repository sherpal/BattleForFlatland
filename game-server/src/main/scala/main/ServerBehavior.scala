package main

import java.util.UUID

import akka.NotUsed
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.{ActorSystem => ClassicActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, UpgradeToWebSocket}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink}
import io.circe.parser.decode
import io.circe.{Decoder, Encoder}
import utils.streams.TypedActorFlow

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Creates a behaviour that will have a root for websockets, decoding incoming (string) messages as In and
  * encoding output message from Out (to string)
  * @tparam In messages coming from the client
  * @tparam Out messages going to the client
  */
trait ServerBehavior[In, Out] {

  import ServerBehavior._

  /**
    * Specify the actor that processes incoming web socket messages.
    * The Behaviour has to sent Out messages to the `outerWorld` actor, which will then be sent to
    * the WebSocket client.
    * Incoming In messages from the client will be sent to the output Behaviour.
    */
  def socketActor(outerWorld: ActorRef[Out]): Behavior[In]

  /**
    * Flow used for the web socket route.
    * Incoming string messages are deserialized into instances of In using Circe, and output Out
    * messages are serialized back into string message using Circe again.
    *
    * Bytes should be treated differently, although the idea would be the same...
    */
  private def webSocketService(viaActor: Flow[In, Out, _])(
      implicit as: ActorSystem[_],
      decoder: Decoder[In],
      encoder: Encoder[Out]
  ): Flow[Message, TextMessage.Strict, NotUsed] =
    Flow[Message]
      .mapAsync(16) {
        case tm: TextMessage =>
          tm.toStrict(1.second)
        case bm: BinaryMessage =>
          bm.dataStream.runWith(Sink.ignore)
          Future.successful(TextMessage(""))
      }
      .map(_.text)
      .wireTap(x => println(x))
      .map(decode[In])
      .alsoTo(Flow[Either[io.circe.Error, In]].collect { case Left(error) => error }.to(Sink.foreach(println)))
      .collect { case Right(in) => in }
      .via(viaActor)
      .map(encoder.apply)
      .map(_.noSpaces)
      .map(TextMessage(_))

  private def requestHandler(
      context: ActorContext[ServerMessage]
  )(implicit decoder: Decoder[In], encoder: Encoder[Out], materializer: Materializer) =
    Flow[HttpRequest].map {
      case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
        //        entity.toStrict(250.millis).map { strictEntity =>
        //          strictEntity.data.utf8String
        //        }
        HttpResponse(entity = "hello")
      case req @ HttpRequest(GET, Uri.Path("/greeter"), _, _, _) =>
        req.header[UpgradeToWebSocket] match {
          case Some(upgrade) =>
            println("connection")
            implicit val as: ActorSystem[_] = context.system
            val actorFlow = TypedActorFlow.actorRefFromContext[In, Out](
              socketActor,
              "Connection" + UUID.randomUUID().toString,
              context
            )
            upgrade.handleMessages(webSocketService(actorFlow))
          case None => HttpResponse(400, entity = "Not a valid websocket request!")
        }
      case r: HttpRequest =>
        r.discardEntityBytes() // important to drain incoming HTTP Entity stream
        HttpResponse(404, entity = "Unknown resource!")
    }

  /** Starts an akka http server with the routes specified above. */
  def apply(host: String, port: Int)(
      implicit
      decoder: Decoder[In],
      encoder: Encoder[Out]
  ): Behavior[ServerMessage] = Behaviors.setup { context =>
    implicit val classic: ClassicActorSystem = context.system.toClassic

    val serverBinding: Future[Http.ServerBinding] =
      Http().bindAndHandle(requestHandler(context), host, port)

    context.pipeToSelf(serverBinding) {
      case Success(_) =>
        Started
      case Failure(exception) =>
        exception.printStackTrace()
        Stop
    }

    Behaviors.receiveMessage {
      case Started =>
        println(s"Server online at http://$host:$port/")
        Behaviors.same
      case Stop =>
        Behaviors.stopped
    }

  }

}

object ServerBehavior {

  sealed trait ServerMessage
  case object Stop extends ServerMessage
  case object Started extends ServerMessage
}
