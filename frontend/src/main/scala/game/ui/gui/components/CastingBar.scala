package game.ui.gui.components

import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.mod.{Application, Container, Graphics, Sprite}

final class CastingBar(
    entityId: Entity.Id,
    uiContainer: Container,
    frameTexture: Texture,
    innerTexture: Texture
) {

  private val frameSprite = new Sprite(frameTexture)
  private val innerSprite = new Sprite(innerTexture)
  innerSprite.tint   = 0xFF0000
  innerSprite.width  = frameSprite.width
  innerSprite.height = frameSprite.height
  private val mask = new Graphics()
  innerSprite.mask = mask

  val containerSprite: Container = {
    val s = new Container
    uiContainer.addChild(s)
    s.addChild(innerSprite)
    s.addChild(frameSprite)
    s.addChild(mask)
    s
  }

  def update(gameState: GameState, currentTime: Long): Unit =
    gameState.castingEntityInfo.get(entityId) match {
      case Some(castingInfo) =>
        containerSprite.visible = true

        val currentCastTime    = (currentTime - castingInfo.startedTime).toDouble
        val castingProgression = currentCastTime / castingInfo.ability.castingTime

        mask.clear().beginFill(0xc0c0c0).drawRect(0, 0, innerSprite.width * castingProgression, innerSprite.height)

      case None =>
        containerSprite.visible = false
    }

}
