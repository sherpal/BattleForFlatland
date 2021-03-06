package game.ui.gui.reactivecomponents

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.{Signal, Val}
import game.ui.reactivepixi.AttributeModifierBuilder._
import game.ui.reactivepixi.EventModifierBuilder._
import game.ui.reactivepixi.ReactivePixiElement._
import gamelogic.abilities.Ability
import gamelogic.entities._
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.anon.Align
import typings.pixiJs.mod.{Rectangle, TextStyle}
import utils.misc.RGBColour

final class TargetFrame(
    maybeTargetIdEvents: Signal[Option[Entity.Id]],
    barTexture: Texture,
    gameStateUpdates: EventStream[(GameState, Long)],
    dimensions: Signal[(Double, Double)],
    targetFromGUIWriter: Observer[Entity.Id],
    abilityIdColourMap: Map[Ability.AbilityId, RGBColour]
) extends GUIComponent {

  val lifeBarProportion = 0.8

  val maybeTargetEvents: EventStream[Option[MovingBody with LivingEntity]] =
    gameStateUpdates.withCurrentValueOf(maybeTargetIdEvents).map {
      case ((gameState, _), maybeEntityId) => maybeEntityId.flatMap(gameState.livingEntityAndMovingBodyById)
    }

  val bar = new StatusBar(
    maybeTargetEvents
      .map {
        _.fold(0.0) { entity =>
          entity.life / entity.maxLife
        }
      }
      .startWith(0.0),
    Val(RGBColour.green),
    Val(true),
    barTexture,
    dimensions.map { case (w, h) => (w, h * lifeBarProportion) }
  )

  val maybeCastingEntityInfoEvents: EventStream[(Option[EntityCastingInfo], Long)] = maybeTargetEvents
    .combineWith(gameStateUpdates)
    .collect {
      case (Some(target), (gs, time)) => (target, gs, time)
    }
    .map {
      case (target, gameState, currentTime) =>
        (gameState.castingEntityInfo.get(target.id), currentTime)
    }

  val maybeFillingRatio: EventStream[Option[Double]] = maybeCastingEntityInfoEvents
    .map {
      case (maybeInfo, currentTime) =>
        maybeInfo.map(info => (currentTime - info.startedTime) / info.castingTime.toDouble)
    }

  val castingBarColour: Signal[RGBColour] = maybeCastingEntityInfoEvents
    .map(_._1)
    .collect {
      case Some(info) => abilityIdColourMap(info.ability.abilityId)
    }
    .toSignal(RGBColour.red)

  val castingBar: ReactiveContainer = new StatusBar(
    maybeFillingRatio.map(_.getOrElse(0.0)).startWith(0.0),
    castingBarColour,
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
      Align().setFontSize(10)
    )
  )

  val lifeText: ReactiveText = pixiText(
    "",
    text <-- maybeTargetEvents.collect { case Some(entity) => entity.life.toInt.toString }.toSignal(""),
    textStyle := new TextStyle(
      Align().setFontSize(15)
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
