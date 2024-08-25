package server

import io.circe.parser.decode
import cask.model.Request
import cask.model.Response.Raw
import cask.router.Result
import zio.*
import zio.Exit.Success
import zio.Exit.Failure
import menus.data.User
import io.circe.syntax.EncoderOps
import errors.ErrorADT

object Server extends cask.MainRoutes with ziocask.WithZIOEndpoints[BackendEnv] {

  val layer = ZLayer.make[BackendEnv](
    services.crypto.Crypto.live
  )

  val runtime: Runtime[BackendEnv] =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.unsafe.fromLayer(layer)
    }

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

  @caskz.getJ[Option[User]]("/api/me")
  def me(request: cask.Request) =
    ZIO.succeed(request.cookies.get("session").map(session => User(session.value)))

  @caskz.post[String]("/api/login")
  def login(request: cask.Request) = for {
    user <- ZIO.fromEither(decode[User](request.text()))
  } yield cask.Response("", 200, Seq(), Seq(cask.Cookie("session", user.name)))

  @cask.get("/api")
  def hello() = "Hello World!"

  @caskz.get[String]("/api/ping/:name")
  def ping(name: String) = ZIO.succeed(cask.Response("PONG: " ++ name))

  @caskz.post[String]("/api/do-thing")
  def doThing(request: cask.Request) =
    for {
      body        <- ZIO.succeed(request.text())
      encodedBody <- services.crypto.hashPassword(body)
    } yield cask.Response(encodedBody.pw)

  @StaticResourcesWithContentType("/static")
  def staticResourceRoutes() = "static"

  initialize()

  println(s"Server listening at $host:$port")
}
