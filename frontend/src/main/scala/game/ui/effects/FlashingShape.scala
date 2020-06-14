package game.ui.effects

import game.Camera
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Polygon, Shape}
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.mod.{Container, Sprite}

final class FlashingShape(
    shape: Shape,
    position: Complex,
    rotation: Double,
    startTime: Long,
    duration: Long,
    camera: Camera,
    texture: Texture
) extends GameEffect {

  val sprite = new Sprite(texture)
  sprite.rotation = -rotation
  sprite.anchor.set(0, 0.5)

  override def addToContainer(container: Container): Unit = container.addChild(sprite)

  def destroy(): Unit =
    sprite.destroy()

  override def isOver(currentTime: Long): Boolean = currentTime - startTime > duration

  def update(currentTime: Long): Unit =
    if (isOver(currentTime)) {
      sprite.visible = false
    } else {
      camera.viewportManager(sprite, position, shape.boundingBox)
    }

}
