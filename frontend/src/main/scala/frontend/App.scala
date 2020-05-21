package frontend

import assets.ScalaLogo
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.raw.Node
import services.http._
import typings.pixiJs.mod.{Application, Sprite, Texture}
import typings.pixiJs.{AnonAntialias => ApplicationOptions}
import urldsl.language.PathSegment.dummyErrorImpl._
import urldsl.language.QueryParameters.dummyErrorImpl.{param => qParam}
import utils.laminarzio.Implicits._
import zio.{UIO, ZIO}

import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions
import scala.scalajs.js
import scala.util.{Success, Try}

object App {

  private val css = AppCSS
  dom.console.log(css.asInstanceOf[js.Object])

  ScalaLogo

  private def postNumber(nbr: Int) =
    post[Int, String](root / "hello", qParam[Int]("nbr"))(nbr).provideLayer(FHttpClient.live)

  val postBus: EventBus[Int] = new EventBus()

  val numberToPost: Var[Int] = Var(0)

  val returned: EventStream[String] = postBus.events
    .map(postNumber)
    .flatMap(EventStream.fromZIOEffect)

  val amISuperUser: ZIO[Any, Throwable, Int] = getStatus(root / "users" / "am-i-super-user")
    .provideLayer(FHttpClient.live)

  implicit def scalaDurationToZIODuration(duration: scala.concurrent.duration.Duration): zio.duration.Duration =
    zio.duration.Duration.fromScala(duration)

  private val addPixiAnimation = for {
    _ <- zio.clock.sleep(FiniteDuration(1, "second"))
    container <- ZIO.effect(Option(dom.document.getElementById("canvas-container").asInstanceOf[dom.html.Div]).get)
    app <- UIO.succeed(new Application(ApplicationOptions(backgroundColor = 0x1099bb)))
    _ <- ZIO.effectTotal {
      val texture = Texture.from(ScalaLogo)

      // create a new Sprite from an image path
      val bunny: Sprite = new Sprite(texture)

      // center the sprite's anchor point
      bunny.anchor.set(0.5)

      // move the sprite to the center of the screen
      bunny.x = app.screen.width / 2
      bunny.y = app.screen.height / 2

      app.stage.addChild(bunny)

      container.appendChild(app.view.asInstanceOf[Node])
    }
  } yield "Pixi animation loaded"

  zio.Runtime.default.unsafeRunAsync(addPixiAnimation.provideLayer(zio.clock.Clock.live))(println)

  def apply(): ReactiveHtmlElement[html.Div] = div(
    className := "App",
    h1("Frontend works!"),
    section(
      h2("Backend works?"),
      p(
        child <-- EventStream
          .fromZIOEffect(get[String](root / "hello").provideLayer(FHttpClient.live))
          .map(span(_))
      )
    ),
    section(
      h2("Post to backend!"),
      input(
        value <-- numberToPost.signal.map(_.toString),
        inContext(
          thisElem =>
            onInput.mapTo(Try(thisElem.ref.value.toInt)).collect { case Success(nbr) => nbr } --> numberToPost.writer
        )
      ),
      button("Click me!", onClick.mapTo(numberToPost.now) --> postBus.writer),
      br(),
      span("Returned: ", child <-- returned.map(identity[String]))
    ),
    section(
      h2("SuperUser works"),
      button("Am I Super", onClick.mapTo(()) --> (_ => zio.Runtime.default.unsafeRunAsync(amISuperUser)(println)))
    ),
    section(
      h2("Pixi works"),
      div(idAttr := "canvas-container")
    )
//    section(
//      h2("Database works?"),
//      p(
//        button(
//          onClick --> (
//              _ =>
//                boilerplate
//                  .response(ignore)
//                  .put(path("models", "insert"))
//                  .body(randomSharedModel())
//                  .send()
//            ),
//          "Insert random element"
//        )
//      ), {
//        val downloadBus = new EventBus[Boolean]()
//
//        p(
//          button("Download models", onClick.mapTo(true) --> downloadBus.writer),
//          ul(
//            children <-- downloadBus.events.flatMap(
//              _ =>
//                EventStream
//                  .fromFuture(
//                    boilerplate
//                      .get(path("models", "get"))
//                      .response(asStringAlways.map(decode[List[SharedModelClass]]))
//                      .send()
//                      .map(_.body.getOrElse(Nil))
//                  )
//                  .map(_.map(_.toString).map(li(_)))
//            )
//          )
//        )
//      }
//    )
  )

}
