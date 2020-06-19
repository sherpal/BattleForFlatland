package game.ui.effects

import game.Camera
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import gamelogic.physics.shape.Shape
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

  override def isOver(currentTime: Long, gameState: GameState): Boolean = currentTime - startTime > duration

  def update(currentTime: Long, gameState: GameState): Unit =
    if (isOver(currentTime, gameState)) {
      sprite.visible = false
    } else {
      camera.viewportManager(sprite, position, shape.boundingBox)
    }

}
