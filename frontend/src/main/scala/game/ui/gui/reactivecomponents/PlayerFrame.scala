package game.ui.gui.reactivecomponents

import assets.Asset
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.{Signal, Val}
import game.ui.gui.reactivecomponents.buffcontainer.BuffContainer
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.{Graphics, LoaderResource, Texture}
import game.ui.reactivepixi.ReactivePixiElement._
import game.ui.reactivepixi.AttributeModifierBuilder._
import game.ui.reactivepixi.EventModifierBuilder._
import game.ui.reactivepixi.PixiModifier
import gamelogic.abilities.WithTargetAbility
import gamelogic.entities.classes.PlayerClass
import gamelogic.physics.Complex
import typings.pixiJs.anon.Align
import typings.pixiJs.mod.{Rectangle, TextStyle}
import utils.misc.RGBColour
import utils.laminarzio.Implicits._

import scala.concurrent.duration._

final class PlayerFrame(
    maybeMyId: Option[Entity.Id], // possibility to give the id of the playing player to check in range distance
    val entityId: Entity.Id,
    entityShapeTexture: Texture,
    lifeTexture: Texture,
    resourceTexture: Texture,
    dimensions: Signal[(Double, Double)], // signal for width and height
    resources: PartialFunction[Asset, LoaderResource],
    targetFromGUIWriter: Observer[Entity.Id],
    gameStateUpdates: EventStream[(GameState, Long)],
    buffIconSize: Double
) extends GUIComponent {

  val maybeEntityEvents: EventStream[Option[PlayerClass]] = gameStateUpdates.map(_._1).map(_.players.get(entityId))
  val entityEvents: EventStream[PlayerClass]              = maybeEntityEvents.collect { case Some(entity) => entity }

  val heightSignal: Signal[Double]    = dimensions.map(_._2)
  val barsWidthSignal: Signal[Double] = dimensions.map { case (w, h) => w - h }

  val entityWithDimensionsEvents: EventStream[(PlayerClass, (Double, Double))] =
    entityEvents.withCurrentValueOf(barsWidthSignal.combineWith(heightSignal))

  val lifeProportion                       = 0.8
  val lifeSpriteHeight: Signal[Double]     = heightSignal.map(_ * lifeProportion)
  val resourceSpriteHeight: Signal[Double] = heightSignal.map(_ * (1 - lifeProportion))

  val maybeOutOfRangeAlphaModifier: PixiModifier[ReactiveSprite] = maybeMyId.map { playingPlayerId =>
    alpha <-- gameStateUpdates
      .throttle(500)
      .map(_._1)
      .map { gameState =>
        (for {
          my    <- gameState.players.get(playingPlayerId)
          their <- gameState.players.get(entityId)
          distance = my.pos distanceTo their.pos
        } yield distance < WithTargetAbility.healRange) match {
          case Some(true)  => 1.0
          case Some(false) => 0.3
          case None        => 1.0
        }
      }
      .toSignal(1.0)
  }

  private def adaptMask(width: Double, height: Double, ratio: Double): Graphics => Unit =
    _.clear().beginFill(0x000000).drawRect(0, 0, width * ratio, height)

  val lifeMask: ReactiveGraphics = pixiGraphics(
    x <-- heightSignal,
    moveGraphics <-- entityWithDimensionsEvents.map {
      case (entity, (width, height)) =>
        adaptMask(width, height, entity.life / entity.maxLife)
    }
  )
  val shapeSprite: ReactiveSprite = pixiSprite(
    entityShapeTexture,
    dims <-- heightSignal.map(h => (h, h)),
    tintInt <-- entityEvents.map(_.colour).toSignal(0)
  )
  val backgroundLifeSprite: ReactiveSprite = pixiSprite(
    lifeTexture,
    tint := RGBColour.gray,
    x <-- heightSignal,
    width <-- barsWidthSignal,
    height <-- lifeSpriteHeight,
    maybeOutOfRangeAlphaModifier
  )
  val lifeSprite: ReactiveSprite = pixiSprite(
    lifeTexture,
    mask := lifeMask,
    tint := RGBColour.green,
    x <-- heightSignal,
    width <-- barsWidthSignal,
    height <-- lifeSpriteHeight,
    maybeOutOfRangeAlphaModifier
  )
  val resourceMask: ReactiveGraphics = pixiGraphics(
    x <-- heightSignal,
    y <-- lifeSpriteHeight,
    moveGraphics <-- entityWithDimensionsEvents.map {
      case (entity, (width, height)) =>
        adaptMask(width, height, entity.resourceAmount.amount / entity.maxResourceAmount)
    }
  )
  val resourceSprite: ReactiveSprite = pixiSprite(
    resourceTexture,
    mask := resourceMask,
    tint <-- entityEvents.map(_.resourceType.colour).toSignal(RGBColour.white),
    x <-- heightSignal,
    y <-- lifeSpriteHeight,
    width <-- barsWidthSignal,
    height <-- resourceSpriteHeight
  )

  val playerNameText: ReactiveText = pixiText(
    "",
    text <-- entityEvents.map(_.name).toSignal(""),
    textStyle := new TextStyle(
      Align(
        fontSize = 10.0
      )
    ),
    x <-- heightSignal.map(_ + 4),
    y := 2,
    tint := RGBColour.white
  )

  val lifeText: ReactiveText = pixiText(
    "",
    text <-- entityEvents.map(_.life.toInt.toString).toSignal(""),
    x <-- dimensions.map(_._1 - 30),
    textStyle := new TextStyle(Align(fontSize = 15.0))
  )

  val buffContainer = new BuffContainer(
    entityId,
    resources,
    gameStateUpdates,
    Val(buffIconSize),
    dimensions.map { case (_, height) => Complex(0, height) }
  )

  container.amend(
    shapeSprite,
    backgroundLifeSprite,
    lifeSprite,
    lifeMask,
    resourceSprite,
    resourceMask,
    playerNameText,
    lifeText,
    buffContainer,
    interactive := true,
    pixiGraphics( // this is used to immediately set the height of the total frame with the buff container
      moveGraphics := (_.beginFill(0, 0).drawRect(0, 0, buffIconSize, buffIconSize).endFill()),
      y <-- dimensions.map(_._2)
    ),
    hitArea <-- dimensions.map { case (width, height) => new Rectangle(0, 0, width, height) },
    onClick.stopPropagation.mapTo(entityId) --> targetFromGUIWriter
  )

}
