package game.ui.gui

import assets.Asset
import assets.ingame.gui.bars.{LiteStepBar, MinimalistBar}
import game.ui.gui.components.gridcontainer.GridContainer
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

  implicit private val playerFrameOrdering: Ordering[PlayerFrame] = Ordering.by(_.entityId)
  val playerFrameGridContainer = new GridContainer[PlayerFrame](
    GridContainer.Row,
    5,
    2
  )
  playerFrameGridContainer.container.x = 0
  playerFrameGridContainer.container.y = 150
  guiContainer.addChild(playerFrameGridContainer.container)

  def update(gameState: GameState, currentTime: Long): Unit = {
    castingBar.update(gameState, currentTime)

    gameState.players.keys.filterNot(playerFrameGridContainer.currentElements.map(_.entityId).contains).foreach {
      entityId =>
        playerFrameGridContainer.addElement(
          new PlayerFrame(entityId, {
            val graphics = new Graphics

            graphics.lineStyle(2, 0xccc).beginFill(0, 0).drawRect(0, 0, 15, 15).endFill()

            application.renderer.generateTexture(graphics, 1, 1)
          }, resources(MinimalistBar).texture, resources(MinimalistBar).texture, 120, 30)
        )
    }

    playerFrameGridContainer.currentElements.foreach(_.update(gameState))
    playerFrameGridContainer.display()
  }

}
