package game.ui.gui

import assets.Asset
import assets.Asset.ingame.gui.abilities._
import assets.Asset.ingame.gui.bars._
import com.raquo.airstream.core.Observer
import com.raquo.airstream.signal.SignalViewer
import game.ui.gui.components.buffs.BuffContainer
import game.ui.gui.components.gridcontainer.GridContainer
import game.ui.gui.components._
import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss101.{BigDot, BigHit, SmallHit}
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
    targetFromGUIWriter: Observer[Entity.Id],
    $maybeTarget: SignalViewer[Option[MovingBody with LivingEntity]],
    useAbilityWriter: Observer[Ability.AbilityId]
) {
  val guiContainer = new Container

  application.stage.addChild(guiContainer)

  /** Casting bar of the player */
  val castingBar = new CastingBar(playerId, guiContainer, {
    val graphics = new Graphics
    graphics.lineStyle(2, 0xccc).beginFill(0, 0).drawRect(0, 0, 200, 15).endFill()
    application.renderer.generateTexture(graphics, 1, 1)
  }, resources(liteStepBar).texture)
  castingBar.container.x = (application.view.width - castingBar.container.width) / 2
  castingBar.container.y = application.view.height - castingBar.container.height

  /** Frame containing the members of the team. */
  implicit private val playerFrameOrdering: Ordering[PlayerFrame] = Ordering.by(_.entityId)
  val playerFrameGridContainer = new GridContainer[PlayerFrame](
    GridContainer.Row,
    5,
    2
  )
  playerFrameGridContainer.container.x = 0
  playerFrameGridContainer.container.y = 150
  guiContainer.addChild(playerFrameGridContainer.container)

  /** Frame containing the information about the target. */
  val targetFrame = new TargetFrame($maybeTarget, resources(minimalistBar).texture)
  guiContainer.addChild(targetFrame.container)
  targetFrame.container.x = application.view.width / 2
  targetFrame.container.y = application.view.height - 70

  /** Frame containing the information about the player */
  val playerFrame = new PlayerFrame(playerId, {
    val graphics = new Graphics

    graphics
      .lineStyle(2, 0xccc)
      .beginFill(0, 0)
      .drawRect(0, 0, 15, 15)
      .endFill()

    application.renderer.generateTexture(graphics, 1, 1)
  }, resources(minimalistBar).texture, resources(minimalistBar).texture, 120, 30, resources, targetFromGUIWriter)
  guiContainer.addChild(playerFrame.container)
  playerFrame.container.x = application.view.width / 2 - 120
  playerFrame.container.y = application.view.height - 70

  /** Buffs of the player */
  var maybePlayerBuffContainer: Option[BuffContainer] = Option.empty

  /** Cooldown bars for the boss */
  implicit private val cdBarOrdering: Ordering[CooldownBar] = Ordering.by(_.abilityId)
  val bossCooldownContainer                                 = new GridContainer[CooldownBar](GridContainer.Row, 10, 1)
  guiContainer.addChild(bossCooldownContainer.container)
  bossCooldownContainer.container.x = application.view.width - 150

  /** Container of all ability buttons */
  implicit private val abilityButtonOrdering: Ordering[AbilityButton] = Ordering.by(_.abilityId)
  val abilityButtonContainer = new GridContainer[AbilityButton](
    GridContainer.Column,
    10,
    1
  )
  abilityButtonContainer.container.y = application.view.height - 30
  guiContainer.addChild(abilityButtonContainer.container)

  private var maybeBossThreatMeter: Option[BossThreatMeter] = Option.empty
  private var maybeBossFrame: Option[BossFrame]             = Option.empty

  def update(gameState: GameState, currentTime: Long): Unit = {
    castingBar.update(gameState, currentTime)

    gameState.players.keys.filterNot(playerFrameGridContainer.currentElements.map(_.entityId).contains).foreach {
      entityId =>
        playerFrameGridContainer.addElement(
          new PlayerFrame(
            entityId, {
              val graphics = new Graphics

              graphics
                .lineStyle(2, 0xccc)
                .beginFill(0, 0)
                .drawRect(0, 0, 15, 15)
                .endFill()

              application.renderer.generateTexture(graphics, 1, 1)
            },
            resources(minimalistBar).texture,
            resources(minimalistBar).texture,
            120,
            30,
            resources,
            targetFromGUIWriter
          )
        )
    }

    if (gameState.bosses.nonEmpty && bossCooldownContainer.isEmpty) {
      val bossId = gameState.bosses.head._1

      List(
        new CooldownBar(
          bossId,
          Ability.boss101SmallHitId,
          SmallHit.name,
          RGBColour.blue,
          resources(minimalistBar).texture
        ),
        new CooldownBar(
          bossId,
          Ability.boss101BigDotId,
          BigDot.name,
          RGBColour.red,
          resources(minimalistBar).texture
        ),
        new CooldownBar(
          bossId,
          Ability.boss101BigHitId,
          BigHit.name,
          RGBColour.green,
          resources(minimalistBar).texture
        )
      ).foreach { bar =>
        bar.setSize(150, 20)
        bossCooldownContainer.addElement(bar)
      }
    }

    if (abilityButtonContainer.isEmpty) {
      gameState.players.get(playerId).foreach { player =>
        val buttons = player.abilities.map { abilityId =>
          new AbilityButton(
            abilityId,
            playerId,
            useAbilityWriter,
            resources(Asset.abilityAssetMap(abilityId)).texture,
            resources(abilityOverlay).texture
          )
        }
        buttons.foreach(_.setSize(30.0))

        abilityButtonContainer.addElements(buttons)
      }
    }

//    maybePlayerBuffContainer = maybePlayerBuffContainer.fold(
//      gameState.players.get(playerId).map(_.id).map { playerId =>
//        val buffContainer = new BuffContainer(playerId, resources)
//        guiContainer.addChild(buffContainer.container)
//        buffContainer.container.x = playerFrame.container.x
//        buffContainer.container.y = playerFrame.container.y - 20.0
//        buffContainer
//      }
//    )(Some(_))

    //maybePlayerBuffContainer.foreach(_.update(gameState, currentTime))

    maybeBossThreatMeter = maybeBossThreatMeter.fold(
      gameState.bosses.headOption.map(_._1).map { bossId =>
        val threatMeter = new BossThreatMeter(bossId, resources(minimalistBar).texture)
        threatMeter.container.x = application.view.width - threatMeter.barWidth
        threatMeter.container.y = application.view.height

        guiContainer.addChild(threatMeter.container)
        threatMeter
      }
    )(Some(_))

    maybeBossFrame = maybeBossFrame.fold(
      gameState.bosses.headOption.map(_._1).map { bossId =>
        val bossFrame = new BossFrame(bossId, {
          val graphics = new Graphics
          graphics.lineStyle(2, 0).beginFill(0, 1).drawRect(0, 0, 200, 15).endFill()
          application.renderer.generateTexture(graphics, 1, 1)
        }, resources(minimalistBar).texture, resources(minimalistBar).texture, targetFromGUIWriter)
        bossFrame.container.x = (application.view.width - bossFrame.container.width) / 2
        guiContainer.addChild(bossFrame.container)
        bossFrame
      }
    )(Some(_))

    maybeBossThreatMeter.foreach(_.update(gameState, currentTime))
    maybeBossFrame.foreach(_.update(gameState, currentTime))

    playerFrameGridContainer.currentElements.foreach(_.update(gameState, currentTime))
    playerFrameGridContainer.display()

    bossCooldownContainer.currentElements.foreach(_.update(gameState, currentTime))
    bossCooldownContainer.display()

    abilityButtonContainer.currentElements.foreach(_.update(gameState, currentTime))
    abilityButtonContainer.display()

    targetFrame.update(gameState, currentTime)
    playerFrame.update(gameState, currentTime)
  }

}
