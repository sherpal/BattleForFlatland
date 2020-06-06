package game

import gamelogic.physics.Complex
import gamelogic.physics.shape.BoundingBox
import org.scalajs.dom.html
import typings.pixiJs.PIXI.{Container, Sprite}

final class Camera(canvas: html.Canvas) {

  /**
    * Change coordinates between a world coordinate and local coordinate for the canvas.
    *
    * Recall that canvas coordinates increase to the right and to the bottom. World coordinates increase to the right and
    * to the top.
    */
  def worldToLocal(z: Complex): Complex = {
    val z0 = z - worldCenter
    Complex(width / 2 + z0.re * scaleX, height / 2 - z0.im * scaleY)
  }

  /**
    * Change coordinates between the mouse position and the world position.
    */
  def mousePosToWorld(z: Complex): Complex = worldCenter + Complex(z.re / scaleX, z.im / scaleY)

  /**
    * Change coordinates from world position to mouse position.
    * This may be used for the GUI Regions.
    */
  def worldToMousePos(w: Complex): Complex = Complex(
    scaleX * (w.re - worldCenter.re),
    scaleY * (w.im - worldCenter.im)
  )

  /**
    * Camera coordinates
    */
  def width: Int  = canvas.width
  def height: Int = canvas.height

  /**
    * World coordinates
    */
  var worldCenter: Complex = Complex(0, 0)
  var worldWidth: Double   = canvas.width
  var worldHeight: Double  = canvas.height

  def left: Double                   = worldCenter.re - worldWidth / 2
  def right: Double                  = worldCenter.re + worldWidth / 2
  def bottom: Double                 = worldCenter.im - worldHeight / 2
  def top: Double                    = worldCenter.im + worldHeight / 2
  def cameraBoundingBox: BoundingBox = BoundingBox(left, top, right, bottom)

  /**
    * World coordinates are multiplied by the scale to know its size in pixel.
    *
    * Example:
    * The canvas is of size 400*300, and the world width is 800. Then, things must be drawn twice as small on the
    * horizontal axis.
    */
  def scaleX: Double = width / worldWidth
  def scaleY: Double = height / worldHeight

  def setScaleX(x: Double): Unit =
    worldWidth = width / x
  def setScaleY(y: Double): Unit =
    worldHeight = height / y
  def setScale(s: Double): Unit = {
    setScaleX(s)
    setScaleY(s)
  }

  private def inView(center: Complex, boundingBox: BoundingBox): Boolean =
    center.re + boundingBox.left <= right && center.re + boundingBox.right >= left &&
      center.im + boundingBox.bottom <= top && center.im + boundingBox.top >= bottom

  /**
    * Gives the visibility, the position and the scales of the sprite.
    *
    * @param sprite      Sprite to give the spec to.
    * @param worldPos    The position in world coordinates at which the sprite must be anchored.
    * @param boundingBox The boundingBox of the shape associated to the sprite, in world coordinates. The center of the
    *                    bounding box must be worldPos.
    */
  def viewportManager(sprite: Sprite, worldPos: Complex, boundingBox: BoundingBox): Unit =
    viewportManager(sprite, worldPos, worldPos, boundingBox)

  /**
    * Gives the visibility, the position and the scales of the sprite.
    *
    * @param container         [[typings.pixiJs.mod.Container]] to give the spec to.
    * @param worldPos          The position in world coordinates at which the sprite must be anchored.
    * @param boundingBoxCenter Center of the boundingBox in world coordinates.
    * @param boundingBox       The boundingBox of the shape associated to the sprite, in world coordinates. The center of
    *                          the bounding box must be worldPos.
    */
  def viewportManager(
      container: Container,
      worldPos: Complex,
      boundingBoxCenter: Complex,
      boundingBox: BoundingBox
  ): Unit =
    if (inView(boundingBoxCenter, boundingBox)) {
      if (!container.visible) {
        container.visible = true
      }

      if (container.scale.x != scaleX || container.scale.y != scaleY) {
        container.scale.set(scaleX, scaleY)
      }

      val Complex(x, y) = worldToLocal(worldPos)
      container.position.set(x, y)
    } else {
      container.visible = false
    }

  def viewportManagerSized(
      container: Container,
      worldPos: Complex,
      worldWidth: Double,
      worldHeight: Double,
      boundingBoxCenter: Complex,
      boundingBox: BoundingBox
  ): Unit =
    if (inView(boundingBoxCenter, boundingBox)) {
      if (!container.visible) {
        container.visible = true
      }

      container.width  = worldWidth * scaleX
      container.height = worldHeight * scaleY

      val Complex(x, y) = worldToLocal(worldPos)
      container.position.set(x, y)

    } else {
      container.visible = false
    }

  /**
    * Same as previous viewportManager, but with the possibility of adding a scale.
    */
  def viewportManager(
      container: Container,
      worldPos: Complex,
      boundingBoxCenter: Complex,
      boundingBox: BoundingBox,
      sx: Double,
      sy: Double
  ): Unit = {
    viewportManager(container, worldPos, boundingBoxCenter, boundingBox)
    container.scale.set(scaleX * sx, scaleY * sy)
  }

  /**
    * Takes a traversable of Sprites with their corresponding position and Shape, and give them visibility, position and
    * scales.
    */
  def viewportManager(spritesWithShapes: Iterable[(Sprite, Complex, BoundingBox)]): Unit =
    spritesWithShapes.foreach({
      case (sprite, worldPos, boundingBox) => viewportManager(sprite, worldPos, boundingBox)
    })

}
