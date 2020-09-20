package game.ui.effects.boss.boss103

import game.Camera
import game.ui.effects.GameEffect
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import gamelogic.physics.shape.BoundingBox
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.mod.{Container, Sprite}

import scala.collection.mutable

final class CleansingNovaEffect(
    casterId: Entity.Id,
    startTime: Long,
    texture: Texture,
    camera: Camera
) extends GameEffect {

  val duration = 500L

  val effectContainer = new Container

  /**
    * Map from the player ids to the position of the sprite, which is the point at the middle of the player and the
    * caster
    */
  val lines: mutable.Map[Entity.Id, (Container, Complex, BoundingBox)] = mutable.Map.empty

  def destroy(): Unit = effectContainer.destroy()

  def update(currentTime: Long, gameState: GameState): Unit =
    gameState.players.valuesIterator
      .map(
        player =>
          (
            player,
            lines.getOrElseUpdate(
              player.id, {
                val tempSprite = new Sprite(texture)

                val casterPosition = gameState.bosses.get(casterId).fold(0: Complex)(_.pos)
                val casterToPlayer = casterPosition - player.pos

                tempSprite.rotation = -casterToPlayer.arg
                tempSprite.scale
                  .set(casterToPlayer.modulus / texture.width, 1)
                tempSprite.anchor.set(0.5)

                val container = new Container
                container.addChild(tempSprite)
                effectContainer.addChild(container)

                val bbTopLeft     = Complex(casterPosition.re min player.pos.re, casterPosition.im max player.pos.im)
                val bbBottomRight = Complex(casterPosition.re max player.pos.re, casterPosition.im min player.pos.im)
                (
                  container,
                  (casterPosition + player.pos) / 2,
                  BoundingBox(bbTopLeft.re, bbBottomRight.im, bbBottomRight.re, bbTopLeft.im)
                )
              }
            )
          )
      )
      .foreach {
        case (_, (container, center, boundingBox)) =>
          camera.viewportManager(container, center, center, boundingBox)
      }

  def isOver(currentTime: Long, gameState: GameState): Boolean =
    (currentTime - startTime > duration) || !gameState.bosses.contains(casterId)

  def addToContainer(container: Container): Unit = container.addChild(effectContainer)
}
