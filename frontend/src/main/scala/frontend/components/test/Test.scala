package frontend.components.test

import assets.Asset.ingame.gui.bars._
import assets.ScalaLogo
import com.raquo.laminar.api.L.{div, EventBus}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import game.GameAssetLoader
import game.ui.reactivepixi.AttributeModifierBuilder
import org.scalajs.dom
import org.scalajs.dom.html
import typings.pixiJs.mod.{Application, Container, Graphics, Sprite}
import zio.ZIO
import game.ui.reactivepixi.ReactivePixiElement._
import game.ui.reactivepixi.AttributeModifierBuilder._
import gamelogic.physics.Complex

import scala.concurrent.duration._
import scala.scalajs.js.timers._

object Test {

  val application = new Application
  val assetLoader = new GameAssetLoader(application)

  private val container = div()

  zio.Runtime.default.unsafeRunToFuture(for {
    resources <- assetLoader.loadAssets
    _ <- ZIO.effectTotal(
      container.ref.appendChild(application.view.asInstanceOf[dom.html.Canvas])
    )
    _ <- ZIO.effectTotal {

      val positionBus = new EventBus[Complex]

      val percentageBus = new EventBus[Double]

      val width_  = 300
      val height_ = 50

      val g = pixiGraphics(
        moveGraphics <-- percentageBus.events.map(coef => _.clear().beginFill().drawRect(0, 0, width_ * coef, height_))
      )

      val s = pixiSprite(
        resources(ScalaLogo).texture,
        width := width_,
        height := height_,
        x <-- positionBus.events.map(_.re),
        y <-- positionBus.events.map(_.im),
        maskChild := g
      )

      stage(application)(s)

      setInterval(16) {
        positionBus.writer.onNext(
          Complex.rotation(System.currentTimeMillis().toDouble / 1000) * 100 +
            Complex(application.view.width, application.view.height) / 2
        )
        percentageBus.writer.onNext(
          (System.currentTimeMillis() % 1000) / 1000.0
        )
      }

    }
  } yield ())

  def apply(): ReactiveHtmlElement[html.Div] = div(
    container
  )

}
