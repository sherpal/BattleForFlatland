package game.ui.effects.boss.boss103

import game.Camera
import game.ui.effects.GameEffect
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import gamelogic.physics.shape.BoundingBox
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.mod.{Container, Sprite}

final class SacredGroundEffect(
    position: Complex,
    radius: Double,
    startTime: Long,
    texture: Texture,
    camera: Camera
) extends GameEffect {
  val duration = 500L

  val effectContainer = new Container

  val effectSprite = new Sprite(texture)
  effectSprite.anchor.set(0.5)
  effectSprite.alpha = 0.3
  effectContainer.addChild(effectSprite)

  val boundingBox: BoundingBox = BoundingBox.fromRadius(radius)

  def destroy(): Unit = effectContainer.destroy()

  def update(currentTime: Long, gameState: GameState): Unit = {
    val currentSize = 2 * radius * (currentTime - startTime) / duration.toDouble
    effectSprite.width  = currentSize
    effectSprite.height = currentSize
    camera.viewportManager(effectContainer, position, position, boundingBox)
  }

  def isOver(currentTime: Long, gameState: GameState): Boolean = currentTime - startTime > duration

  def addToContainer(container: Container): Unit = container.addChild(effectContainer)
}
