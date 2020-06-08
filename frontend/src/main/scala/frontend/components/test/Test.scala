package frontend.components.test

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.utils.laminarutils.reactChild
import frontend.components.utils.tailwind._
import game.GameAssetLoader
import org.scalajs.dom.html
import slinky.core.facade.ReactInstance
import typings.pixiJs.mod.Application
import typings.reactColor.mod.ColorResult
import utils.misc.{Colour, RGBAColour, RGBColour}
import laika.api._
import laika.ast.{Emphasized, Header, Id, NoOpt, Options, Styles, Title}
import laika.format._
import laika.markdown.github.GitHubFlavor

object Test {

  val application = new Application
  val assetLoader = new GameAssetLoader(application)

  val transformer = Transformer
    .from(Markdown)
    .to(HTML)
    .rendering {
      case (fmt, Title(content, opt)) =>
        fmt.element("h1", opt.id.fold[Options](NoOpt)(Id(_)) + Styles("text-3xl"), content)
      case (fmt, Header(2, content, opt)) =>
        fmt.element("h2", opt.id.fold[Options](NoOpt)(Id(_)) + Styles("text-2xl text-teal-300"), content)
//      case truc =>
//        println(truc)
//        "dummy"
    }
    .build

  val fileContent: String =
    """
      |# Technical python quizz
      |
      |This is the *first* (perhaps the last) technical quizz! This quizz will be about python, and will ask 13 questions with three choices each.
      |
      |## Why python?
      |
      |Python and TypeScript are probably the two most used languages at B12, so I expect many people to have at least touched the language. Why not TS? Well, because I don't know the language enough to create interesting questions. Also, TypeScript is compiled, which means that there are far less quirks at runtime, and it's easier to master (unless of course you put `any` everywhere).
      |
      |```scala
      |This is code
      |```
      |
      |Une liste:
      |
      |- something
      |- other things
      |
      |With numbers:
      |
      |1. something
      |2. other thing
      |
      |""".stripMargin

  private val container = div()

//  zio.Runtime.default.unsafeRunToFuture(for {
//    resources <- assetLoader.loadAssets.tap(x => ZIO.effectTotal(dom.console.log(x)))
//    _ <- ZIO.effectTotal(
//      container.ref.appendChild(application.view.asInstanceOf[dom.html.Canvas])
//    )
//    _ <- ZIO.effectTotal {
//      val sprite = new Sprite(resources(xeonBar).texture)
//      application.stage.addChild(sprite)
//      sprite.tint = 0xFF0000
//    }
//    _ <- ZIO.effectTotal {
//      val container = new Container
//      application.stage.addChild(container)
//      container.y = 50
//
//      val sprite = new Sprite(resources(liteStepBar).texture)
//      container.addChild(sprite)
//      sprite.tint = 0x00FF00
//
//      val mask = new Graphics()
//        .beginFill(0xc0c0c0)
//        .drawRect(0, 0, sprite.width, sprite.height)
//
//      container.addChild(mask)
//
//      sprite.mask = mask
//
//      var xScale = 0
////      setInterval(100.millis) {
////
////        xScale = (xScale + 1) % 100
////        println(xScale)
////
////        mask.clear().beginFill(0xc0c0c0).drawRect(0, 0, sprite.width * xScale / 100, sprite.height)
////
////      }
//    }
//  } yield ())

  def colorResultToRGBColour(color: ColorResult): RGBAColour =
    RGBColour(color.rgb.r.toInt, color.rgb.g.toInt, color.rgb.b.toInt).withAlpha(color.rgb.a.getOrElse(1.0))

  val colourBus = new EventBus[Colour]

  def apply(): ReactiveHtmlElement[html.Div] = div(
    button(
      btn,
      btnBlue,
      "A Button!"
    ),
    div(
      width := "50px",
      height := "30px",
      backgroundColor <-- colourBus.events.startWith(RGBColour.white).map(_.rgb)
    ),
    onMountCallback { _ =>
      println("mounting 2!")
    },
    div(
      onMountCallback { ctx =>
        ctx.thisNode.ref.innerHTML = transformer.transform(fileContent) match {
          case Left(value)  => "<pre>" + value.toString + "</pre>"
          case Right(value) => value
        }
      }
    )
  )

}
