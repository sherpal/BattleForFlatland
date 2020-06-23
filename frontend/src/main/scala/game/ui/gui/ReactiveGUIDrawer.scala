package game.ui.gui

import assets.Asset
import assets.Asset.ingame.gui.bars.liteStepBar
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.{Signal, Val}
import game.ui.reactivepixi.ReactiveStage
import gamelogic.abilities.Ability
import gamelogic.entities.{Entity, LivingEntity, MovingBody}
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.LoaderResource
import game.ui.reactivepixi.ReactivePixiElement._
import game.ui.reactivepixi.AttributeModifierBuilder._
import game.ui.reactivepixi.ChildrenReceiver._
import gamelogic.physics.Complex
import reactivecomponents._
import typings.pixiJs.mod.Graphics
import assets.Asset.ingame.gui.abilities._
import assets.Asset.ingame.gui.bars._
import game.ui.gui.reactivecomponents.gridcontainer.GridContainer

final class ReactiveGUIDrawer(
    playerId: Entity.Id,
    stage: ReactiveStage,
    resources: PartialFunction[Asset, LoaderResource],
    targetFromGUIWriter: Observer[Entity.Id],
    $maybeTarget: Signal[Option[MovingBody with LivingEntity]],
    useAbilityWriter: Observer[Ability.AbilityId],
    gameStateUpdates: EventStream[(GameState, Long)]
) {

  val guiContainer: ReactiveContainer = pixiContainer()
  stage(guiContainer)

  stage.amend(new FPSDisplay(gameStateUpdates.map(_ => ())))

  val castingBarDimensions: Val[(Double, Double)] = Val((200.0, 15.0))

  /** Casting bar of the player */
  val castingBar = new CastingBar(
    playerId,
    guiContainer,
    stage.resizeEvents.map {
      case (width, height) =>
        Complex(
          (width - castingBarDimensions.now._1) / 2,
          height - castingBarDimensions.now._2
        )
    },
    castingBarDimensions, {
      val graphics = new Graphics
      graphics.lineStyle(2, 0xccc).beginFill(0, 0).drawRect(0, 0, 200, 15).endFill()
      stage.application.renderer.generateTexture(graphics, 1, 1)
    },
    resources(liteStepBar).texture,
    gameStateUpdates
  )
  guiContainer.amend(castingBar)

  val targetFrame: ReactiveContainer = new TargetFrame(
    $maybeTarget.map(_.map(_.id)),
    resources(minimalistBar).texture,
    gameStateUpdates,
    Val((150, 40)),
    targetFromGUIWriter
  ).amend(
    position <-- stage.resizeEvents.map {
      case (viewWidth, viewHeight) =>
        Complex(viewWidth / 2, viewHeight - 70)
    }
  )
  guiContainer.amend(targetFrame)

  val bossFrameDimensions: Val[(Double, Double)] = Val((200.0, 15.0))
  guiContainer.amend(
    children <-- gameStateUpdates
      .map(_._1.bosses.valuesIterator.map(_.id).toList.sorted)
      .toSignal(Nil)
      .split(identity) {
        case (id, _, _) =>
          new reactivecomponents.BossFrame(id, {
            val graphics = new Graphics
            graphics.lineStyle(2, 0).beginFill(0, 1).drawRect(0, 0, 200, 15).endFill()
            stage.application.renderer.generateTexture(graphics, 1, 1)
          }, resources(minimalistBar).texture, resources(minimalistBar).texture, targetFromGUIWriter, gameStateUpdates, bossFrameDimensions)
            .amend(x <-- stage.resizeEvents.map(_._1).combineWith(bossFrameDimensions.map(_._1)).map {
              case (viewWidth, frameWidth) =>
                (viewWidth - frameWidth) / 2
            })
      }
  )

  val playerFrameGridContainer: ReactiveContainer = new GridContainer(
    GridContainer.Row,
    gameStateUpdates
      .map(_._1.players.valuesIterator.map(_.id).toList.sorted)
      .toSignal(Nil)
      .split(identity) {
        case (entityId, _, _) =>
          new PlayerFrame(
            entityId, {
              val graphics = new Graphics

              graphics
                .lineStyle(2, 0xccc)
                .beginFill(0, 0)
                .drawRect(0, 0, 15, 15)
                .endFill()

              stage.application.renderer.generateTexture(graphics, 1, 1)
            },
            resources(minimalistBar).texture,
            resources(minimalistBar).texture,
            Val((120.0, 30.0)),
            resources,
            targetFromGUIWriter,
            gameStateUpdates
          ): ReactiveContainer
      },
    5
  ).amend(
    y := 150
  )

  guiContainer.amend(playerFrameGridContainer)

}
