package main

import java.util.UUID

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.{ActorSystem => ClassicActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, UpgradeToWebSocket}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink}
import akka.util.Timeout
import authentication.TokenBearer
import errors.ErrorADT
import io.circe.parser.decode
import io.circe.{Decoder, Encoder}
import models.bff.ingame.GameUserCredentials
import models.users.User
import utils.streams.TypedActorFlow
import zio.{Has, ZIO, ZLayer}
import urldsl.language.PathSegment.dummyErrorImpl._
import urldsl.language.QueryParameters.simpleParamErrorImpl._
import io.circe.generic.auto._
import urldsl.language.{PathSegment, PathSegmentWithQueryParams}
import urldsl.vocabulary.UrlMatching
import io.circe.syntax._

import scala.concurrent.{ExecutionContext, Future, TimeoutException}
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

  object ServerRoutes {

    final val tokenRoute = root / "api" / "token"

    final val connectWithTokenRoute = (root / "ws" / "connect") ? (param[String]("token") & param[String]("userId")).?

    def doesMatch(uri: Uri, path: PathSegment[_, _]): Boolean =
      path.matchPath(uri.path.toString).isRight

    def doesMatch(uri: Uri, pathAndQuery: PathSegmentWithQueryParams[_, _, _, _]): Boolean =
      pathAndQuery.matchPathAndQueryOption(uri.path.toString, uri.queryString().getOrElse("")).isDefined

  }
  import ServerRoutes._

  private def requestHandler(
      context: ActorContext[ServerMessage],
      tokenBearer: ActorRef[TokenBearer.Message]
  )(implicit decoder: Decoder[In], encoder: Encoder[Out], materializer: Materializer) = {
    implicit val ec: ExecutionContext = context.executionContext
    Flow[HttpRequest].mapAsync(1) {
      case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
        Future.successful(HttpResponse(entity = "hello"))
      case HttpRequest(GET, Uri.Path("/stop"), _, _, _) =>
        context.self ! Stop
        Future.successful(HttpResponse(entity = "stopped"))
      case HttpRequest(POST, uri, _, entity, _) if doesMatch(uri, tokenRoute) =>
        entity
          .toStrict(250.millis)
          .flatMap { strictEntity =>
            val bodyAsString = strictEntity.data.utf8String
            decode[GameUserCredentials](bodyAsString) match {
              case Right(credentials) =>
                tokenBearer
                  .ask[Option[String]](
                    replyTo =>
                      TokenBearer.TokenForUser(
                        credentials,
                        replyTo
                      )
                  )(Timeout(250.millis), context.system.scheduler)
                  .map {
                    case Some(token) => HttpResponse(entity = token)
                    case None =>
                      HttpResponse(StatusCodes.Forbidden, entity = ErrorADT.WrongGameCredentials.asJson.spaces2)
                  }
              case Left(error) =>
                error.fillInStackTrace()
                error.printStackTrace()
                Future.successful(HttpResponse(status = StatusCodes.BadRequest, entity = error.getMessage))
            }
          }
      case req @ HttpRequest(GET, uri, _, _, _) if doesMatch(uri, connectWithTokenRoute) =>
        connectWithTokenRoute.matchPathAndQueryOption(uri.path.toString, uri.queryString().getOrElse("")) match {
          case Some(UrlMatching(_, Some((token, userId)))) =>
            tokenBearer
              .ask[Boolean](TokenBearer.UserConnects(userId, token, _))(Timeout(250.millis), context.system.scheduler)
              .map {
                if (_) req.header[UpgradeToWebSocket] match {
                  case Some(upgrade) =>
                    implicit val as: ActorSystem[_] = context.system
                    val actorFlow = TypedActorFlow.actorRefFromContext[In, Out](
                      socketActor,
                      "Connection" + UUID.randomUUID().toString,
                      context
                    )
                    upgrade.handleMessages(webSocketService(actorFlow))
                  case None => HttpResponse(400, entity = "Not a valid web socket request!")
                } else HttpResponse(StatusCodes.Forbidden, entity = "wrong token")
              }
          case Some(UrlMatching(_, None)) =>
            Future.successful(HttpResponse(StatusCodes.BadRequest, entity = "missing user id and/or token"))
          case _ =>
            Future.successful(HttpResponse(StatusCodes.BadRequest, entity = "bad"))
        }

      case r: HttpRequest =>
        r.discardEntityBytes() // important to drain incoming HTTP Entity stream
        Future.successful(HttpResponse(404, entity = "Unknown resource!"))
    }
  }

  /** Starts an akka http server with the routes specified above. */
  private def apply(host: String, port: Int)(
      implicit
      decoder: Decoder[In],
      encoder: Encoder[Out]
  ): Behavior[ServerMessage] = Behaviors.setup { context =>
    implicit val classic: ClassicActorSystem = context.system.toClassic

    val tokenBearer = context.spawn(TokenBearer(), "TokenBearer")

    val serverBinding: Future[Http.ServerBinding] =
      Http().bindAndHandle(requestHandler(context, tokenBearer), host, port)

    context.pipeToSelf(serverBinding) {
      case Success(_) =>
        Started
      case Failure(exception) =>
        exception.printStackTrace()
        Stop
    }

    def started(waitingForStoppedNotification: Option[ActorRef[Unit]]): Behavior[ServerMessage] =
      Behaviors.receiveMessage {
        case Started => Behaviors.unhandled // should not happen
        case Stop =>
          waitingForStoppedNotification.foreach(_ ! ())
          Behaviors.stopped
        case WarnMeWhenStarted(replyTo) =>
          replyTo ! ()
          Behaviors.same
        case ReceivedCredentials(users, credentials) =>
          println("Forward credentials to token bearer.")
          tokenBearer ! TokenBearer.CredentialsWrapper(users, credentials)
          Behaviors.same
        case WarnMeWhenStopped(replyTo) =>
          started(Some(replyTo))
      }

    def notYetStarted(
        waitingForStartedNotification: Option[ActorRef[Unit]],
        waitingForStoppedNotification: Option[ActorRef[Unit]]
    ): Behavior[ServerMessage] =
      Behaviors.receiveMessage {
        case Started =>
          println(s"Server online at http://$host:$port/")
          waitingForStartedNotification.foreach(_ ! ())
          started(waitingForStoppedNotification)
        case Stop =>
          waitingForStartedNotification.foreach(_ ! ())
          Behaviors.stopped
        case WarnMeWhenStarted(replyTo) =>
          notYetStarted(Some(replyTo), waitingForStoppedNotification)
        case ReceivedCredentials(users, credentials) =>
          tokenBearer ! TokenBearer.CredentialsWrapper(users, credentials)
          Behaviors.same
        case WarnMeWhenStopped(replyTo) =>
          notYetStarted(waitingForStartedNotification, Some(replyTo))
      }

    notYetStarted(None, None)

  }

  def launchServer(
      host: String,
      port: Int,
      actorName: String = "Server"
  )(
      implicit
      decoder: Decoder[In],
      encoder: Encoder[Out]
  ): ZLayer[Any, Nothing, Has[ActorSystem[ServerMessage]]] =
    ZLayer.fromAcquireRelease(
      for {
        actorSystem <- ZIO.effectTotal(ActorSystem(apply(host, port), actorName))
        _ <- ZIO
          .fromFuture { _ =>
            actorSystem.ask[Unit](replyTo => WarnMeWhenStarted(replyTo))(Timeout(5.seconds), actorSystem.scheduler)
          }
          .catchAll(_ => ZIO.effectTotal(actorSystem ! Stop))
      } yield actorSystem
    )(system => ZIO.effectTotal(system ! Stop))

  /**
    * Program that will constantly ask the server `ref` to tell when it is stop, and completes thereof.
    * Since the ask pattern requires a timeout, we retry each time a TimeoutException occur
    */
  def waitForServerToStop(ref: ActorSystem[ServerMessage]): ZIO[Any, Nothing, Unit] =
    for {
      _ <- ZIO
        .fromFuture { _ =>
          ref.ask[Unit](replyTo => WarnMeWhenStopped(replyTo))(Timeout(5.seconds), ref.scheduler)
        }
        .refineOrDie {
          case e: TimeoutException => e
        }
        .catchAll { _ =>
          waitForServerToStop(ref)
        }
    } yield ()

}

object ServerBehavior {

  sealed trait ServerMessage
  case object Stop extends ServerMessage
  case object Started extends ServerMessage
  private case class WarnMeWhenStarted(replyTo: ActorRef[Unit]) extends ServerMessage
  case class WarnMeWhenStopped(replyTo: ActorRef[Unit]) extends ServerMessage
  case class ReceivedCredentials(users: List[User], credentials: List[GameUserCredentials]) extends ServerMessage

}
