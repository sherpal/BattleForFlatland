package game.ui

import gamelogic.physics.Complex
import typings.pixiJs.PIXI
import typings.pixiJs.mod.{Application, Graphics, Point}

import scala.scalajs.js.|
import scala.scalajs.js.JSConverters._

trait Drawer {

  def application: Application

  protected def diskTexture(
      colour: Int,
      alpha: Double,
      radius: Double,
      withBlackDot: Boolean = false
  ): PIXI.RenderTexture = {
    val graphics = new Graphics
    graphics.lineStyle(0) // draw a circle, set the lineStyle to zero so the circle doesn't have an outline

    graphics.beginFill(colour, alpha)
    graphics.drawCircle(0, 0, radius)
    graphics.endFill()

    if (withBlackDot) {
      graphics.beginFill(0x000000, 1)
      graphics.drawCircle(radius, 0.0, 3.0)
      graphics.endFill()
    }

    application.renderer.generateTexture(graphics, 1, 1)
  }

  protected def circleTexture(colour: Int, alpha: Double, radius: Double): PIXI.RenderTexture = {
    val graphics = new Graphics
    graphics.lineStyle(1, colour, alpha)

    graphics.beginFill(0xFFFFFF, 0.0)
    graphics.drawCircle(0, 0, radius)

    application.renderer.generateTexture(graphics, 1, 1)

  }

  protected def polygonTexture(colour: Int, shape: gamelogic.physics.shape.Polygon): PIXI.RenderTexture = {
    val graphics = new Graphics
    graphics
      .lineStyle(0)
      .beginFill(colour, 1)
      .drawPolygon(
        shape.vertices
          .map {
            case Complex(re, im) => new Point(re, im)
          }
          .toJSArray
          .asInstanceOf[scala.scalajs.js.Array[Double | typings.pixiJs.PIXI.Point]]
      )

    application.renderer.generateTexture(graphics, 1, 1)
  }

}
