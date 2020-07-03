package game.ui.gui

import assets.Asset
import assets.Asset.ingame.gui.bars.liteStepBar
import com.raquo.airstream.core.{Observable, Observer, Transaction}
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
import com.raquo.airstream.features.SingleParentObservable
import game.ui.gui.reactivecomponents.gridcontainer.GridContainer
import game.ui.gui.reactivecomponents.threatmeter.BossThreatMeter
import utils.misc.RGBColour
import utils.laminarzio.Implicits._

import scala.concurrent.duration._

final class ReactiveGUIDrawer(
    playerId: Entity.Id,
    stage: ReactiveStage,
    resources: PartialFunction[Asset, LoaderResource],
    targetFromGUIWriter: Observer[Entity.Id],
    $maybeTarget: Signal[Option[MovingBody with LivingEntity]],
    useAbilityWriter: Observer[Ability.AbilityId],
    gameStateUpdates: EventStream[(GameState, Long)]
) {

  private val blackTexture = {
    val graphics = new Graphics

    graphics
      .lineStyle(2, 0)
      .beginFill(0, 0)
      .drawRect(0, 0, 32, 32)
      .endFill()

    stage.application.renderer.generateTexture(graphics, 1, 1)
  }

  val slowGameStateUpdates: EventStream[(GameState, Long)] = gameStateUpdates.spacedBy(500.millis)

  val guiContainer: ReactiveContainer = pixiContainer()
  stage(guiContainer)

  guiContainer.amend(new FPSDisplay(gameStateUpdates))
  guiContainer.amend(new ClockDisplay(slowGameStateUpdates, Val(Complex(10, 50))))

  private val playerFrameDimensions = Val((120.0, 30.0))
  val playerFrame: ReactiveContainer = new PlayerFrame(
    playerId, {
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
    playerFrameDimensions,
    resources,
    targetFromGUIWriter,
    gameStateUpdates,
    20
  ).amend(
    position <-- stage.resizeEvents.map {
      case (viewWidth, viewHeight) =>
        Complex(viewWidth / 2 - playerFrameDimensions.now._1, viewHeight - 70)
    }
  )
  guiContainer.amend(playerFrame)

  val abilityButtonContainer: ReactiveContainer = new GridContainer(
    GridContainer.Column,
    gameStateUpdates
      .map(_._1.players.get(playerId))
      .collect { case Some(player) => player.abilities }
      .toSignal(Set())
      .map { abilities =>
        abilities.toList.map { abilityId =>
          new AbilityButton(
            abilityId,
            playerId,
            useAbilityWriter,
            resources(Asset.abilityAssetMap(abilityId)).texture,
            resources(abilityOverlay).texture,
            gameStateUpdates,
            Val((32, 32))
          ): ReactiveContainer
        }
      },
    10
  ).amend(
    y <-- stage.resizeEvents.map(_._2 - 32)
  )
  guiContainer.amend(abilityButtonContainer)

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

  // todo: change with events when new player appears
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
            gameStateUpdates,
            15
          ): ReactiveContainer
      },
    5
  ).amend(
    y := 150
  )
  guiContainer.amend(playerFrameGridContainer)

  // todo: change with events when new boss appears
  guiContainer.amend(
    children <--
      gameStateUpdates
        .map(_._1.bosses.keys.toList.headOption)
        .collect {
          case Some(bossId) => bossId
        }
        .map(List(_))
        .toSignal(Nil)
        .map { bossIds =>
          bossIds.map { bossId =>
            new BossThreatMeter(
              bossId,
              resources(minimalistBar).texture,
              slowGameStateUpdates,
              stage.resizeEvents
            )

          }
        }
  )

  private val bossCDBarsSize = Val((150.0, 20.0))
  guiContainer.amend(
    children <-- gameStateUpdates
      .map(_._1.bosses.valuesIterator.toList.headOption.map(boss => (boss.id, boss.abilityNames)))
      .toSignal(Option.empty)
      .map {
        case None => List.empty[ReactiveContainer]
        case Some((bossId, abilityNames)) =>
          abilityNames.zipWithIndex.map {
            case ((abilityId, name), idx) =>
              println("creating cooldown bar")
              new CooldownBar(
                bossId,
                abilityId,
                name,
                RGBColour.someColours(idx % RGBColour.someColours.length),
                resources(minimalistBar).texture,
                gameStateUpdates,
                bossCDBarsSize
              ).amend(
                position <-- stage.resizeEvents.combineWith(bossCDBarsSize).map {
                  case ((viewWidth, _), (width, height)) =>
                    Complex(viewWidth - width, idx * height)
                }
              )
          }.toList
      }
  )

}
