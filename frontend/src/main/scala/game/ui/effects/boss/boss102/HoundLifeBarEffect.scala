package game.ui.effects.boss.boss102

import game.Camera
import game.ui.effects.GameEffect
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss102.BossHound
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.mod.{Container, Graphics, Sprite}
import utils.misc.RGBColour

import scala.util.Random

final class HoundLifeBarEffect(
    entityId: Entity.Id,
    startTime: Long,
    backgroundTexture: Texture,
    barTexture: Texture,
    camera: Camera
) extends GameEffect {

  import gamelogic.physics.Complex._

  private val width  = BossHound.shape.radius * 2
  private val height = 5.0

  private val randomOffset = Complex(Random.between(-2, 2), Random.between(-2, 2))

  private val red    = RGBColour.red.intColour
  private val orange = RGBColour.orange.intColour
  private val green  = RGBColour.green.intColour

  private val barContainer: Container = new Container
  private val background              = new Sprite(backgroundTexture)
  private val bar                     = new Sprite(barTexture)
  private val mask                    = new Graphics
  bar.mask = mask
  barContainer.addChild(background)
  background.width  = width
  background.height = height
  barContainer.addChild(bar)
  bar.width  = width
  bar.height = height
  barContainer.addChild(mask)

  /** Returns a value between 0 and 1, depending on the life percentage amount of the hound. */
  def computeValue(hound: BossHound): Double =
    hound.life / hound.maxLife

  def destroy(): Unit = barContainer.destroy()

  def update(currentTime: Long, gameState: GameState): Unit =
    gameState.entityByIdAs[BossHound](entityId).fold[Unit](barContainer.visible = false) { hound =>
      if (!barContainer.visible) barContainer.visible = true

      val lifePercentage = computeValue(hound)

      bar.tint = lifePercentage match {
        case x if x <= 0.2 => red
        case x if x <= 0.5 => orange
        case _             => green
      }

      mask
        .clear()
        .beginFill(0xc0c0c0)
        .drawRect(0, 0, bar.width * lifePercentage, bar.height)

      val houndPosition  = hound.currentPosition(currentTime)
      val verticalOffset = (hound.shape.radius + height).i
      camera.viewportManager(
        barContainer,
        houndPosition - width / 2 + verticalOffset + height.i / 2 + randomOffset,
        houndPosition + verticalOffset,
        hound.shape.boundingBox
      )
    }

  def isOver(currentTime: Long, gameState: GameState): Boolean =
    currentTime > startTime && !gameState.entities.contains(entityId)

  def addToContainer(container: Container): Unit = container.addChild(barContainer)
}
