package frontend.components.test

import assets.ingame.gui.bars.{LiteStepBar, XeonBar}
import com.raquo.laminar.api.L._
import frontend.components.utils.tailwind._
import game.GameAssetLoader
import org.scalajs.dom
import typings.pixiJs.mod.{Application, Container, Graphics, Sprite}
import zio.ZIO

import scala.concurrent.duration._
import scala.scalajs.js.timers._

object Test {

  val application = new Application
  val assetLoader = new GameAssetLoader(application)

  private val container = div()

  zio.Runtime.default.unsafeRunToFuture(for {
    resources <- assetLoader.loadAssets.tap(x => ZIO.effectTotal(dom.console.log(x)))
    _ <- ZIO.effectTotal(
      container.ref.appendChild(application.view.asInstanceOf[dom.html.Canvas])
    )
    _ <- ZIO.effectTotal {
      val sprite = new Sprite(resources(XeonBar).texture)
      application.stage.addChild(sprite)
      sprite.tint = 0xFF0000
    }
    _ <- ZIO.effectTotal {
      val container = new Container
      application.stage.addChild(container)
      container.y = 50

      val sprite = new Sprite(resources(LiteStepBar).texture)
      container.addChild(sprite)
      sprite.tint = 0x00FF00

      val mask = new Graphics()
        .beginFill(0xc0c0c0)
        .drawRect(0, 0, sprite.width, sprite.height)

      container.addChild(mask)

      sprite.mask = mask

      var xScale = 0
      setInterval(100.millis) {

        xScale = (xScale + 1) % 100
        println(xScale)

        mask.clear().beginFill(0xc0c0c0).drawRect(0, 0, sprite.width * xScale / 100, sprite.height)

      }
    }
  } yield ())

  def apply() = div(
    button(
      btn,
      btnBlue,
      "A Button!"
    ),
    container
  )

}
