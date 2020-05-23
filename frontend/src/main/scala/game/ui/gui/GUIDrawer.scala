package game.ui.gui

import assets.Asset
import assets.Asset.ingame.gui.bars._
import com.raquo.airstream.signal.SignalViewer
import game.ui.gui.components.gridcontainer.GridContainer
import game.ui.gui.components.{CastingBar, PlayerFrame, TargetFrame}
import gamelogic.entities.{Entity, LivingEntity, MovingBody}
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.LoaderResource
import typings.pixiJs.mod.{Application, Container, Graphics}

final class GUIDrawer(
    playerId: Entity.Id,
    application: Application,
    resources: PartialFunction[Asset, LoaderResource],
    $maybeTarget: SignalViewer[Option[MovingBody with LivingEntity]]
) {

  val guiContainer = new Container

  application.stage.addChild(guiContainer)

  val castingBar = new CastingBar(playerId, guiContainer, {
    val graphics = new Graphics

    graphics.lineStyle(2, 0xccc).beginFill(0, 0).drawRect(0, 0, 200, 15).endFill()

    application.renderer.generateTexture(graphics, 1, 1)
  }, resources(liteStepBar).texture)

  implicit private val playerFrameOrdering: Ordering[PlayerFrame] = Ordering.by(_.entityId)
  val playerFrameGridContainer = new GridContainer[PlayerFrame](
    GridContainer.Row,
    5,
    2
  )
  playerFrameGridContainer.container.x = 0
  playerFrameGridContainer.container.y = 150
  guiContainer.addChild(playerFrameGridContainer.container)

  val targetFrame = new TargetFrame($maybeTarget, resources(minimalistBar).texture)
  guiContainer.addChild(targetFrame.container)
  targetFrame.container.x = (application.view.width - targetFrame.container.width) / 2
  targetFrame.container.y = application.view.height - 30

  def update(gameState: GameState, currentTime: Long): Unit = {
    castingBar.update(gameState, currentTime)

    gameState.players.keys.filterNot(playerFrameGridContainer.currentElements.map(_.entityId).contains).foreach {
      entityId =>
        playerFrameGridContainer.addElement(
          new PlayerFrame(entityId, {
            val graphics = new Graphics

            graphics.lineStyle(2, 0xccc).beginFill(0, 0).drawRect(0, 0, 15, 15).endFill()

            application.renderer.generateTexture(graphics, 1, 1)
          }, resources(minimalistBar).texture, resources(minimalistBar).texture, 120, 30)
        )
    }

    playerFrameGridContainer.currentElements.foreach(_.update(gameState))
    playerFrameGridContainer.display()

    targetFrame.update(gameState, currentTime)
  }

}
