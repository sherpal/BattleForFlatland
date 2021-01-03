package game.ui.effects.targetmanager

import com.raquo.airstream.ownership.Owner
import game.ui.GameDrawer
import com.raquo.airstream.signal.Signal
import gamelogic.entities.Entity
import com.raquo.airstream.signal.Var
import typings.pixiJs.PIXI.DisplayObject
import scala.scalajs.js
import game.Camera

import typings.pixiFilterGlow.mod.GlowFilter
import typings.pixiFilterOutline.mod.OutlineFilter
import typings.pixiFilterGlow.PIXI.filters.GlowFilterOptions
import gamelogic.entities.MovingBody
import gamelogic.entities.LivingEntity
import com.raquo.airstream.eventstream.EventStream
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.Texture
import game.ui.reactivepixi.ReactivePixiElement._
import game.ui.reactivepixi.AttributeModifierBuilder._
import game.ui.gui.reactivecomponents.StatusBar
import utils.misc.RGBColour
import com.raquo.airstream.signal.Val
import gamelogic.physics.shape.BoundingBox
import gamelogic.physics.Complex._
import gamelogic.physics.Complex

/**
  * The [[TargetManager]] is simply responsible to attach effects to the
  * [[DisplayObject]] representing the current target.
  *
  * The way it's implemented here is clearly not immutable, but who cares?
  * I think it's more performant that way.
  *
  * @param gameDrawer the [[GameDrawer]] from which we can query the [[DisplayObject]]
  * @param maybeTargetSignal the [[Signal]] of current targets
  */
final class TargetManager(
    gameDrawer: GameDrawer,
    maybeTargetSignal: Signal[Option[MovingBody with LivingEntity]],
    gameStateUpdates: EventStream[(GameState, Long)],
    guiContainer: ReactiveContainer,
    backgroundTexture: Texture,
    barTexture: Texture,
    camera: Camera
)(implicit owner: Owner) {
  val barHeight = 7.0

  private val definedTargetStream = maybeTargetSignal.changes
    .collect { case Some(target) => target }

  private val positioningInfo = gameStateUpdates
    .withCurrentValueOf(maybeTargetSignal)
    .collect { case ((gs, time), Some(target)) => (gs.livingEntityAndMovingBodyById(target.id), time) }
    .collect {
      case (Some(target), time) =>
        val targetPosition =
          target.currentPosition(time) + (target.shape.radius + barHeight).i

        val boundingBox = BoundingBox(-1, -1, 1, 1)

        camera.effectViewportManager(targetPosition, boundingBox)
    }

  private val dimensionSignal =
    definedTargetStream
      .map(target => (target.shape.radius * 2, barHeight))
      .withCurrentValueOf(positioningInfo.collect { case (_, Some(scale), _) => scale }.toSignal((1.0, 1.0)))
      .map {
        case ((width, height), (scaleX, scaleY)) => (width * scaleX, height * scaleY)
      }
      .toSignal((10.0, barHeight))

  private val targetLifeSignal = gameStateUpdates
    .withCurrentValueOf(maybeTargetSignal)
    .collect {
      case ((gs, _), Some(target)) => gs.livingEntityAndMovingBodyById(target.id)
    }
    .collect { case Some(target) => target }
    .map(target => target.life / target.maxLife)
    .toSignal(0.0)

  val lifeAboveContainer = pixiContainer(
    visible <-- maybeTargetSignal
      .combineWith(positioningInfo.map { case (b, _, _) => b }.toSignal(false))
      .map {
        case (Some(_), true) => true
        case _               => false
      },
    //scaleXY <-- positioningInfo.collect { case (_, Some(xy), _) => xy },
    //anchor <-- Val(0.5),
    position <-- positioningInfo
      .collect {
        case (_, Some(scale), Some(z)) => (scale, z)
      }
      .withCurrentValueOf(dimensionSignal)
      .map {
        case ((scale, z), (width, height)) =>
          z - Complex(width * scale._1 / 2, height * scale._2 / 2)
      },
    width  <-- dimensionSignal.map(_._1),
    height <-- dimensionSignal.map(_._2),
    pixiSprite(
      backgroundTexture,
      dims <-- dimensionSignal
    ),
    new StatusBar(
      targetLifeSignal,
      Val(RGBColour.green),
      Val(true),
      barTexture,
      dimensionSignal
    )
  )
  guiContainer.amend(lifeAboveContainer)

  private var maybeCurrentTarget = Option.empty[DisplayObject]

  private val targetFilter = new GlowFilter(
    GlowFilterOptions()
      .setOuterStrength(15)
      .setDistance(8)
      .setInnerStrength(1)
      .setColor(0x999999)
      .setQuality(0.5)
  )

  maybeTargetSignal.foreach { (next: Option[MovingBody with LivingEntity]) =>
    val maybeDisplayObject = next.map(_.id).flatMap(gameDrawer.maybeEntityDisplayObjectById)
    maybeCurrentTarget.foreach(_.filters = js.Array())
    maybeCurrentTarget = maybeDisplayObject
    maybeCurrentTarget.foreach(_.filters = js.Array(targetFilter))
  }

}
