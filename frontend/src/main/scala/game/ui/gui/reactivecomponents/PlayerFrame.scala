package game.ui.gui.reactivecomponents

import assets.Asset
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.Signal
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.{Graphics, LoaderResource, Texture}
import game.ui.reactivepixi.ReactivePixiElement._
import game.ui.reactivepixi.AttributeModifierBuilder._
import game.ui.reactivepixi.EventModifierBuilder._
import gamelogic.entities.classes.PlayerClass
import typings.pixiJs.anon.Align
import typings.pixiJs.mod.{Rectangle, TextStyle}
import utils.misc.RGBColour

final class PlayerFrame(
    val entityId: Entity.Id,
    entityShapeTexture: Texture,
    lifeTexture: Texture,
    resourceTexture: Texture,
    dimensions: Signal[(Double, Double)], // signal for width and height
    resources: PartialFunction[Asset, LoaderResource],
    targetFromGUIWriter: Observer[Entity.Id],
    gameStateUpdates: EventStream[(GameState, Long)]
) extends GUIComponent {

  val maybeEntityEvents: EventStream[Option[PlayerClass]] = gameStateUpdates.map(_._1).map(_.players.get(entityId))
  val entityEvents: EventStream[PlayerClass]              = maybeEntityEvents.collect { case Some(entity) => entity }
  val entityWithDimensionsEvents: EventStream[(PlayerClass, (Double, Double))] =
    entityEvents.withCurrentValueOf(dimensions)

  val heightSignal: Signal[Double]    = dimensions.map(_._2)
  val barsWidthSignal: Signal[Double] = dimensions.map { case (w, h) => w - h }

  val lifeProportion                       = 0.8
  val lifeSpriteHeight: Signal[Double]     = heightSignal.map(_ * lifeProportion)
  val resourceSpriteHeight: Signal[Double] = heightSignal.map(_ * (1 - lifeProportion))

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
    dims <-- heightSignal.map(h => (h, h))
  )
  val backgroundLifeSprite: ReactiveSprite = pixiSprite(
    lifeTexture,
    tint := RGBColour.gray,
    x <-- heightSignal,
    width <-- barsWidthSignal,
    height <-- lifeSpriteHeight
  )
  val lifeSprite: ReactiveSprite = pixiSprite(
    lifeTexture,
    mask := lifeMask,
    tint := RGBColour.green,
    x <-- heightSignal,
    width <-- barsWidthSignal,
    height <-- lifeSpriteHeight
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
    text <-- entityEvents.map(_.life.toInt.toString).toSignal(""),
    textStyle := new TextStyle(
      Align(
        fontSize = 10.0
      )
    )
  )

  val lifeText: ReactiveText = pixiText(
    "",
    x <-- dimensions.map(_._2 - 30),
    textStyle := new TextStyle(Align(fontSize = 15.0))
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
    interactive := true,
    hitArea <-- dimensions.map { case (width, height) => new Rectangle(0, 0, width, height) },
    onClick.stopPropagation.mapTo(entityId) --> targetFromGUIWriter
  )

}
