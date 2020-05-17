package game.ui.gui

import assets.Asset
import assets.ingame.gui.bars.{LiteStepBar, MinimalistBar}
import game.ui.gui.components.{CastingBar, PlayerFrame}
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.LoaderResource
import typings.pixiJs.mod.{Application, Container, Graphics}

final class GUIDrawer(
    playerId: Entity.Id,
    application: Application,
    resources: PartialFunction[Asset, LoaderResource]
) {

  val guiContainer = new Container

  application.stage.addChild(guiContainer)

  val castingBar = new CastingBar(playerId, guiContainer, {
    val graphics = new Graphics

    graphics.lineStyle(2, 0xccc).beginFill(0, 0).drawRect(0, 0, 200, 15).endFill()

    application.renderer.generateTexture(graphics, 1, 1)
  }, resources(LiteStepBar).texture)

  val myMainFrame = new PlayerFrame(playerId, {
    val graphics = new Graphics

    graphics.lineStyle(2, 0xccc).beginFill(0, 0).drawRect(0, 0, 15, 15).endFill()

    application.renderer.generateTexture(graphics, 1, 1)
  }, resources(MinimalistBar).texture, resources(MinimalistBar).texture, 120, 15)

  myMainFrame.container.x = 0
  myMainFrame.container.y = 150
  guiContainer.addChild(myMainFrame.container)

  def update(gameState: GameState, currentTime: Long): Unit = {
    castingBar.update(gameState, currentTime)
    myMainFrame.update(gameState)
  }

}
