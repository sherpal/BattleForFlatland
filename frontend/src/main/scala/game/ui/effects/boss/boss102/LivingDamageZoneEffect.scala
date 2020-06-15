package game.ui.effects.boss.boss102

import game.Camera
import game.ui.effects.GameEffect
import gamelogic.buffs.Buff
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import gamelogic.physics.shape.BoundingBox
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.mod.{Container, Sprite}

final class LivingDamageZoneEffect(
    bearerId: Entity.Id,
    buffId: Buff.Id,
    startTime: Long,
    texture: Texture,
    camera: Camera,
    radius: Double
) extends GameEffect {

  val sprite: Sprite = new Sprite(texture)
  sprite.anchor.set(0.5)

  val boundingBox: BoundingBox = BoundingBox(-radius, -radius, radius, radius)

  def destroy(): Unit = sprite.destroy()

  def update(currentTime: Long, gameState: GameState): Unit =
    gameState
      .livingEntityAndMovingBodyById(bearerId)
      .fold[Unit] {
        sprite.visible = false
      } { bearer =>
        sprite.visible = true
        camera.viewportManager(sprite, bearer.currentPosition(currentTime), boundingBox)
      }

  def isOver(currentTime: Long, gameState: GameState): Boolean =
    currentTime >= startTime && gameState.buffById(bearerId, buffId).isEmpty

  def addToContainer(container: Container): Unit = container.addChild(sprite)
}
