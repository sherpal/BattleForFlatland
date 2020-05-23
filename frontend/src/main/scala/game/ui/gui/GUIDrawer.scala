package game.ui.gui

import assets.Asset
import assets.Asset.ingame.gui.bars._
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.SignalViewer
import game.ui.gui.components.gridcontainer.GridContainer
import game.ui.gui.components.{CastingBar, CooldownBar, PlayerFrame, TargetFrame}
import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss101.{BigDot, BigHit}
import gamelogic.entities.{Entity, LivingEntity, MovingBody}
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.LoaderResource
import typings.pixiJs.PIXI.interaction.{InteractionEvent, InteractionEventTypes}
import typings.pixiJs.mod.{Application, Container, Graphics}
import utils.misc.RGBColour

final class GUIDrawer(
    playerId: Entity.Id,
    application: Application,
    resources: PartialFunction[Asset, LoaderResource],
    targetFromGUIWriter: Observer[MovingBody with LivingEntity],
    $gameState: SignalViewer[GameState],
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
  targetFrame.container.x = application.view.width / 2
  targetFrame.container.y = application.view.height - 30

  val playerFrame = new PlayerFrame(playerId, {
    val graphics = new Graphics

    graphics
      .lineStyle(2, 0xccc)
      .beginFill(0, 0)
      .drawRect(0, 0, 15, 15)
      .endFill()

    application.renderer.generateTexture(graphics, 1, 1)
  }, resources(minimalistBar).texture, resources(minimalistBar).texture, 120, 30)
  guiContainer.addChild(playerFrame.container)
  playerFrame.container.x           = application.view.width / 2 - 120
  playerFrame.container.y           = application.view.height - 30
  playerFrame.container.interactive = true
  playerFrame.container.addListener(
    InteractionEventTypes.click, { (_: InteractionEvent) =>
      $gameState.now.players.get(playerId).foreach { entity =>
        println(entity)
        scala.scalajs.js.timers.setTimeout(100.0) {
          targetFromGUIWriter.onNext(entity)
        }
      }
    }
  )

  implicit private val cdBarOrdering: Ordering[CooldownBar] = Ordering.by(_.abilityId)

  val bossCooldownContainer = new GridContainer[CooldownBar](GridContainer.Row, 10, 1)
  guiContainer.addChild(bossCooldownContainer.container)
  bossCooldownContainer.container.x = application.view.width - 150

  def update(gameState: GameState, currentTime: Long): Unit = {
    castingBar.update(gameState, currentTime)

    gameState.players.keys.filterNot(playerFrameGridContainer.currentElements.map(_.entityId).contains).foreach {
      entityId =>
        playerFrameGridContainer.addElement(
          new PlayerFrame(entityId, {
            val graphics = new Graphics

            graphics
              .lineStyle(2, 0xccc)
              .beginFill(0, 0)
              .drawRect(0, 0, 15, 15)
              .endFill()

            application.renderer.generateTexture(graphics, 1, 1)
          }, resources(minimalistBar).texture, resources(minimalistBar).texture, 120, 30)
        )
    }

    if (gameState.bosses.nonEmpty && bossCooldownContainer.isEmpty) {
      val bossId = gameState.bosses.head._1

      List(
        new CooldownBar(
          bossId,
          Ability.boss101BigDotId,
          BigDot.name,
          RGBColour(255, 0, 0),
          resources(minimalistBar).texture
        ),
        new CooldownBar(
          bossId,
          Ability.boss101BigHitId,
          BigHit.name,
          RGBColour(0, 255, 0),
          resources(minimalistBar).texture
        )
      ).foreach { bar =>
        bar.setSize(150, 20)
        bossCooldownContainer.addElement(bar)
      }
    }

    playerFrameGridContainer.currentElements.foreach(_.update(gameState))
    playerFrameGridContainer.display()

    bossCooldownContainer.currentElements.foreach(_.update(gameState, currentTime))
    bossCooldownContainer.display()

    targetFrame.update(gameState, currentTime)
    playerFrame.update(gameState)
  }

}
