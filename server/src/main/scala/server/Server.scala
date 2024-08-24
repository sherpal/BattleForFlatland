package server

import io.circe.parser.decode
import cask.model.Request
import cask.model.Response.Raw
import cask.router.Result
import zio.*
import zio.Exit.Success
import zio.Exit.Failure

object Server extends cask.MainRoutes {

  val layer = ZLayer.make[BackendEnv](
    services.crypto.Crypto.live
  )

  val runtime: Runtime[BackendEnv] =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.unsafe.fromLayer(layer)
    }

  def runEffect[R >: BackendEnv, E <: Throwable, A](effect: ZIO[R, E, A]): A =
    Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe.run(effect)
    } match
      case Success(value) => value
      case Failure(cause) => throw cause.squashTrace

  override def port: Int = Option(java.lang.System.getProperty("port")).fold(9000)(_.toInt)

  override def host: String = Option(java.lang.System.getProperty("isProd"))
    .map(_.toBoolean)
    .fold("127.0.0.1")(isProd => if isProd then "0.0.0.0" else "127.0.0.1")

  @cask.get("/")
  def index() =
    cask.StaticResource(
      "static/index.html",
      getClass.getClassLoader,
      List("Content-Type" -> "text/html; charset=utf-8")
    )

  @cask.get("/api")
  def hello() = "Hello World!"

  @cask.get("/api/ping")
  def ping() = "PONG2"

  @cask.post("/api/do-thing")
  def doThing(request: cask.Request) = runEffect(
    for {
      body        <- ZIO.succeed(request.text())
      encodedBody <- services.crypto.hashPassword(body)
    } yield encodedBody.pw
  )

  @StaticResourcesWithContentType("/static")
  def staticResourceRoutes() = "static"

  initialize()

  println(s"Server listening at $host:$port")
}
