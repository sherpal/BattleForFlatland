package game.ui

import gamelogic.physics.Complex
import typings.pixiJs.PIXI
import typings.pixiJs.mod.{Application, Graphics, Point}
import typings.pixiJs.PIXI.DisplayObject

import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|
import gamelogic.entities.Entity
import typings.pixiJs.mod.Sprite

trait Drawer {

  val linearScale = 1.0 //.asInstanceOf[typings.pixiJs.PIXI.SCALE_MODES.LINEAR]

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

    application.renderer.generateTexture(graphics, linearScale, 1)
  }

  protected def circleTexture(colour: Int, alpha: Double, radius: Double): PIXI.RenderTexture = {
    val graphics = new Graphics
    graphics.lineStyle(1, colour, alpha)

    graphics.beginFill(0xFFFFFF, 0.0)
    graphics.drawCircle(0, 0, radius)

    application.renderer.generateTexture(graphics, linearScale, 1)

  }

  protected def polygonTexture(
      colour: Int,
      alpha: Double,
      shape: gamelogic.physics.shape.Polygon
  ): PIXI.RenderTexture = {
    val graphics = new Graphics
    graphics
      .lineStyle(0)
      .beginFill(colour, alpha)
      .drawPolygon(
        shape.vertices
          .map(_.conjugate)
          .map {
            case Complex(re, im) => new Point(re, im)
          }
          .toJSArray
          .asInstanceOf[scala.scalajs.js.Array[Double | typings.pixiJs.PIXI.Point]]
      )

    application.renderer.generateTexture(graphics, linearScale, 1)
  }

  def redimensionTexture(
      texture: typings.pixiJs.PIXI.Texture,
      width: Double,
      height: Double
  ): typings.pixiJs.PIXI.Texture = {
    val s = new Sprite(texture)
    s.width  = width
    s.height = height

    application.renderer.generateTexture(s, linearScale, 1)
  }

  /**
    * When defined, returns the [[DisplayObject]] reprensenting, in the game, the
    * entity with the given id.
    */
  def maybeEntityDisplayObjectById(entityId: Entity.Id): Option[DisplayObject]

}
