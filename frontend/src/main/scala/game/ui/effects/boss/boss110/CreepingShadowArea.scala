package game.ui.effects.boss.boss110

import game.Camera
import game.ui.effects.GameEffect
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import gamelogic.physics.shape.BoundingBox
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.mod.{Container, Sprite}
import gamelogic.entities.boss.boss110.CreepingShadow
import typings.pixiJs.mod.Graphics
import utils.misc.RGBColour

final class CreepingShadowArea(
    entityId: Entity.Id,
    startTime: Long,
    camera: Camera
) extends GameEffect {

  val sprite   = new Sprite
  val graphics = new Graphics
  sprite.addChild(graphics)

  val colour = RGBColour.black.intColour
  val alpha  = 1.0

  def destroy(): Unit = graphics.destroy()

  def update(currentTime: Long, gameState: GameState): Unit =
    for {
      creepingShadow <- CreepingShadow.extractCreepingShadow(gameState, entityId)
    } {
      val radius   = creepingShadow.radius
      val position = creepingShadow.currentPosition(currentTime)

      graphics.clear()
      graphics.lineStyle(2, colour, alpha)
      graphics.beginFill(0xFFFFFF, 0.0)
      graphics.drawCircle(0, 0, radius)
      camera.viewportManager(sprite, position, creepingShadow.shape.boundingBox)
    }

  def isOver(currentTime: Long, gameState: GameState): Boolean = gameState.ended

  def addToContainer(container: Container): Unit = container.addChild(sprite)

}
