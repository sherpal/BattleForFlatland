package bffclient.menu

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js

import bffclient.lua.{HttpModule, HttpResponse}
import io.circe.{Decoder, Encoder}
import io.circe.parser.decode
import errors.ErrorADT
import menus.data.APIResponse

final class HttpError(message: String) extends Exception(message)

/** The menu server answered with an error (e.g. "you are not the game creator"). */
final class ApiError(val error: ErrorADT) extends Exception(error.repr)

/** JSON over HTTP, with the session cookie of the menu server.
  *
  * Requests run on a LÖVE thread; their responses are polled every frame by a timer, which
  * completes the corresponding futures.
  */
final class HttpClient(baseUrl: String)(using ExecutionContext) {
  private var cookie  = ""
  private val pending = mutable.Map.empty[Int, Promise[HttpResponse]]
  // of the downloads
  private val progress = mutable.Map.empty[Int, Double => Unit]
  private var poller: Option[js.timers.SetIntervalHandle] = None

  def sessionCookie: String = cookie

  private def poll(): Unit = {
    var response = HttpModule.poll()
    while response != null do {
      response.received.toOption match {
        case Some(received) => progress.get(response.id).foreach(_(received))
        case None =>
          if response.setCookie != null && response.setCookie.nonEmpty then
            cookie = response.setCookie.takeWhile(_ != ';')
          progress.remove(response.id)
          pending.remove(response.id).foreach(_.success(response))
      }
      response = HttpModule.poll()
    }
    if pending.isEmpty then {
      poller.foreach(js.timers.clearInterval)
      poller = None
    }
  }

  private def request(
      method: String,
      path: String,
      body: String,
      file: String = "",
      onProgress: Double => Unit = _ => ()
  ): Future[String] = {
    val promise = Promise[HttpResponse]()
    val id      = HttpModule.requestFile(method, baseUrl + path, body, cookie, file)
    pending(id) = promise
    if file.nonEmpty then progress(id) = onProgress
    if poller.isEmpty then poller = Some(js.timers.setInterval(1)(poll()))
    promise.future.flatMap { response =>
      if response.status == 200 then Future.successful(response.body)
      else if response.status == 0 then
        Future.failed(HttpError(s"$method $path failed: ${response.error}"))
      else Future.failed(HttpError(s"$method $path: HTTP ${response.status} ${response.body}"))
    }
  }

  private def decoded[B: Decoder](body: String): Future[B] =
    Future.fromTry(
      decode[B](body).left.map(e => HttpError(s"Cannot decode $body: ${e.getMessage}")).toTry
    )

  def get[B: Decoder](path: String): Future[B] =
    request("GET", path, "").flatMap(decoded[B])

  /** GET of a text that is not JSON. */
  def getText(path: String): Future[String] = request("GET", path, "")

  /** Writes what is at this path to `file` (a path of the operating system), telling `onProgress`
    * the bytes received so far, every now and then.
    */
  def download(path: String, file: String, onProgress: Double => Unit = _ => ()): Future[Unit] =
    request("GET", path, "", file, onProgress).map(_ => ())

  /** POST without response body. */
  def post[A: Encoder](path: String, data: A): Future[Unit] =
    request("POST", path, Encoder[A].apply(data).noSpaces).map(_ => ())

  /** POST returning an `APIResponse[B]`, the menu server's usual answer (failed with an
    * [[ApiError]]).
    */
  def postApi[A: Encoder, B](path: String, data: A)(using Encoder[B], Decoder[B]): Future[B] =
    request("POST", path, Encoder[A].apply(data).noSpaces)
      .flatMap(decoded[APIResponse[B]])
      .flatMap(response => Future.fromTry(response.toEither.left.map(ApiError(_)).toTry))
}
