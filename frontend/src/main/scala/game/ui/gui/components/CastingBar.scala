package game.ui.gui.components

import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.RenderTexture
import typings.pixiJs.mod.{Application, Container, Sprite, Texture}

final class CastingBar(
    entityId: Entity.Id,
    uiContainer: Container,
    frameTexture: RenderTexture,
    innerTexture: RenderTexture
) {

  private val frameSprite = new Sprite(frameTexture)
  private val innerSprite = new Sprite(innerTexture)

  val containerSprite: Container = {
    val s = new Container
    uiContainer.addChild(s)
    s.addChild(innerSprite)
    s.addChild(frameSprite)
    s
  }

  def update(gameState: GameState, currentTime: Long): Unit =
    gameState.castingEntityInfo.get(entityId) match {
      case Some(castingInfo) =>
        containerSprite.visible = true

        val currentCastTime    = (currentTime - castingInfo.startedTime).toDouble
        val castingProgression = currentCastTime / castingInfo.ability.castingTime

        innerSprite.scale.x = castingProgression

      case None =>
        containerSprite.visible = false
    }

}
