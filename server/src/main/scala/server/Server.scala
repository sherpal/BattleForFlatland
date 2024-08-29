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
import cask.main.Routes

object Server extends cask.MainRoutes with ziocask.WithZIOEndpoints[BackendEnv] {

  val layer = ZLayer.make[BackendEnv](
    services.crypto.Crypto.live,
    services.menugames.MenuGames.live,
    services.localstorage.BLocalStorage.live,
    services.events.Events.live
  )

  val runtime: Runtime[BackendEnv] =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.unsafe.fromLayer(layer)
    }

  override def allRoutes: Seq[Routes] = Vector(this, MenuGameRoutes()(using runtime))

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

  @caskz.getJ[Option[User]]("/api/users/me")
  def me(request: cask.Request) =
    ZIO.succeed(request.cookies.get("session").map(session => User(session.value)))

  @caskz.get[String]("/api/oupsy")
  def oupsy() =
    zio.ZIO
      .succeed(throw new RuntimeException("bleh"))
      .catchAllCause(cause => zio.ZIO.succeed(cask.Response(cause.squashTrace.getMessage())))

  @caskz.post[String]("/api/users/login")
  def login(request: cask.Request) = for {
    user <- ZIO.fromEither(decode[User](request.text()))
  } yield cask.Response("", 200, Seq(), Seq(cask.Cookie("session", user.name, path = "/")))

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
