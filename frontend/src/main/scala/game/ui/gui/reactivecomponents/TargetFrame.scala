package game.ui.gui.reactivecomponents

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.{Signal, Val}
import gamelogic.entities.{Entity, LivingEntity, MovingBody, WithName}
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.Texture
import utils.misc.RGBColour
import game.ui.reactivepixi.ReactivePixiElement._
import game.ui.reactivepixi.AttributeModifierBuilder._
import typings.pixiJs.anon.Align
import typings.pixiJs.mod.{Rectangle, TextStyle}
import game.ui.reactivepixi.EventModifierBuilder._
import org.scalajs.dom

final class TargetFrame(
    maybeTargetIdEvents: Signal[Option[Entity.Id]],
    barTexture: Texture,
    gameStateUpdates: EventStream[(GameState, Long)],
    dimensions: Signal[(Double, Double)],
    targetFromGUIWriter: Observer[Entity.Id]
) extends GUIComponent {

  val lifeBarProportion = 0.8

  val maybeTargetEvents: EventStream[Option[MovingBody with LivingEntity]] =
    gameStateUpdates.withCurrentValueOf(maybeTargetIdEvents).map {
      case ((gameState, _), maybeEntityId) => maybeEntityId.flatMap(gameState.livingEntityAndMovingBodyById)
    }

  val bar = new StatusBar(
    maybeTargetEvents.map {
      _.fold(0.0) { entity =>
        entity.life / entity.maxLife
      }
    },
    Val(RGBColour.green),
    Val(true),
    barTexture,
    dimensions.map { case (w, h) => (w, h * lifeBarProportion) }
  )

  val maybeFillingRatio: EventStream[Option[Double]] = maybeTargetEvents
    .combineWith(gameStateUpdates)
    .collect {
      case (Some(target), (gs, time)) => (target, gs, time)
    }
    .map {
      case (target, gameState, currentTime) =>
        gameState.castingEntityInfo.get(target.id).map { info =>
          (currentTime - info.startedTime) / info.castingTime.toDouble
        }
    }

  val castingBar: ReactiveContainer = new StatusBar(
    maybeFillingRatio.map(_.getOrElse(0.0)),
    Val(RGBColour.red),
    maybeFillingRatio.map(_.isDefined).toSignal(false),
    barTexture,
    dimensions.map { case (w, h) => (w, h * (1 - lifeBarProportion)) }
  ).amend(y <-- dimensions.map(_._2).map(_ * lifeBarProportion))

  val nameText: ReactiveText = pixiText(
    "",
    text <-- maybeTargetEvents
      .collect { case Some(entity) => entity }
      .map {
        case entity: WithName => entity.name
        case entity           => s"Entity ${entity.id}"
      }
      .toSignal(""),
    textStyle := new TextStyle(
      Align(
        fontSize = 10.0
      )
    )
  )

  val lifeText: ReactiveText = pixiText(
    "",
    text <-- maybeTargetEvents.collect { case Some(entity) => entity.life.toInt.toString }.toSignal(""),
    textStyle := new TextStyle(
      Align(
        fontSize = 15.0
      )
    ),
    x <-- dimensions.map(_._1 - 40)
  )

  container.amend(
    hitArea <-- dimensions.map { case (width, height) => new Rectangle(0, 0, width, height) },
    interactive := true,
    // Note the `.get`: this is not an issue as if there is not target, it's impossible to click on it.
    onClick.stopPropagation.withCurrentValueOf(maybeTargetIdEvents).map(_._2.get) --> targetFromGUIWriter,
    visible <-- maybeTargetEvents.map(_.isDefined),
    bar,
    castingBar,
    lifeText,
    nameText
  )

}
