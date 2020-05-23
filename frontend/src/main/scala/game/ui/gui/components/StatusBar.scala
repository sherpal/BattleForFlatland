package game.ui.gui.components

import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.mod.{Graphics, Sprite}

/**
  * Helper class for displaying bar on screen.
  *
  * As extending [[game.ui.gui.components.GUIComponent]], you have access to the `container` member to set dimensions
  * and alpha.
  *
  * @param computeValue computes the percentage of filling that this bar must have for the given game state (number
  *                     between 0 and 1.
  * @param computeColour computes the tint value the bar must have for the given game state
  * @param isVisible determine whether this bar should be visible at that game state
  * @param texture texture to draw the bar.
  */
final class StatusBar(
    computeValue: (GameState, Long) => Double,
    computeColour: (GameState, Long) => Int,
    isVisible: (GameState, Long) => Boolean,
    texture: Texture
) extends GUIComponent {

  private val barSprite = new Sprite(texture)
  private val mask      = new Graphics
  barSprite.mask = mask
  container.addChild(barSprite)
  container.addChild(mask)

  def setSize(width: Double, height: Double): Unit = {
    barSprite.width  = width
    barSprite.height = height
  }

  def update(gameState: GameState, currentTime: Long): Unit =
    if (isVisible(gameState, currentTime)) {
      container.visible = true

      barSprite.tint = computeColour(gameState, currentTime)

      mask
        .clear()
        .beginFill(0xc0c0c0)
        .drawRect(0, 0, barSprite.width * computeValue(gameState, currentTime), barSprite.height)

    } else {
      container.visible = false
    }

}
