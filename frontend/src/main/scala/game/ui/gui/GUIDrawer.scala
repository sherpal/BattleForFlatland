package game.ui.gui

import game.ui.gui.components.CastingBar
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import typings.pixiJs.mod.{Application, Container, Graphics}

final class GUIDrawer(playerId: Entity.Id, application: Application) {

  val guiContainer = new Container

  application.stage.addChild(guiContainer)

  val castingBar = new CastingBar(playerId, guiContainer, {
    val graphics = new Graphics

    graphics.lineStyle(2, 0xccc).beginFill(0, 0).drawRect(0, 0, 100, 20).endFill()

    application.renderer.generateTexture(graphics, 1, 1)
  }, {
    val graphics = new Graphics
    graphics
      .lineStyle(0)
      .beginFill(0x0000FF, 1)
      .drawRect(0, 0, 100, 20)
      .endFill()

    application.renderer.generateTexture(graphics, 1, 1)
  })

  def update(gameState: GameState, currentTime: Long): Unit =
    castingBar.update(gameState, currentTime)

}
