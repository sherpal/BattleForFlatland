package game.ui.gui.reactivecomponents

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.Signal
import game.ui.reactivepixi.AttributeModifierBuilder._
import game.ui.reactivepixi.EventModifierBuilder._
import game.ui.reactivepixi.ReactivePixiElement._
import gamelogic.abilities.Ability.AbilityId
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.{Entity, EntityCastingInfo}
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.anon.Align
import typings.pixiJs.mod.TextStyle
import utils.misc.RGBColour

final class BossFrame(
    val entityId: Entity.Id,
    backgroundTexture: Texture,
    lifeTexture: Texture,
    castingBarTexture: Texture,
    targetFromGUIWriter: Observer[Entity.Id],
    gameStateUpdates: EventStream[(GameState, Long)],
    dimensions: Signal[(Double, Double)],
    abilityColourMap: Map[AbilityId, RGBColour]
) extends GUIComponent {

  val maybeBossEvents: EventStream[Option[BossEntity]] = gameStateUpdates.map(_._1).map(_.bosses.get(entityId))
  val bossEvents: EventStream[BossEntity]              = maybeBossEvents.collect { case Some(boss) => boss }

  val maybeCastingInfoEvents: EventStream[Option[(EntityCastingInfo, Long)]] =
    gameStateUpdates.map { case (gameState, time) => gameState.castingEntityInfo.get(entityId).map((_, time)) }

  val widthSignal: Signal[Double]  = dimensions.map(_._1)
  val heightSignal: Signal[Double] = dimensions.map(_._2)

  val lifeSpriteHeight: Signal[Double] = heightSignal.map(_ * 2 / 3)
  val castingBarHeight: Signal[Double] = heightSignal.map(_ * 1 / 3)

  val lifeMask: ReactiveGraphics = pixiGraphics(
    moveGraphics <-- bossEvents.withCurrentValueOf(widthSignal).withCurrentValueOf(lifeSpriteHeight).map {
      case ((boss, width), height) =>
        _.clear().beginFill(0xc0c0c0).drawRect(0, 0, width * boss.life / boss.maxLife, height)
    }
  ) // lifeMask

  val castingBarMask: ReactiveGraphics = pixiGraphics( // castingBarMask
    moveGraphics <-- maybeCastingInfoEvents
      .collect {
        case Some((castingInfo, currentTime)) => (castingInfo, currentTime)
      }
      .withCurrentValueOf(widthSignal)
      .withCurrentValueOf(castingBarHeight)
      .map {
        case (((castingInfo, currentTime), width), height) =>
          val currentCastTime    = (currentTime - castingInfo.startedTime).toDouble
          val castingProgression = currentCastTime / castingInfo.ability.castingTime
          _.clear().beginFill(0xc0c0c0).drawRect(0, 0, width * castingProgression, height)
      },
    y <-- lifeSpriteHeight
  )

  container.amend(
    interactive := true,
    onClick.stopPropagation.mapTo(entityId) --> targetFromGUIWriter,
    pixiSprite( // backgroundSprite
      backgroundTexture,
      dims <-- dimensions
    ),
    pixiSprite( // lifeSprite
      lifeTexture,
      width <-- widthSignal,
      height <-- lifeSpriteHeight,
      tint := RGBColour.green,
      mask := lifeMask
    ),
    lifeMask,
    pixiSprite( // castingBar
      castingBarTexture,
      visible <-- maybeCastingInfoEvents.map(_.isDefined).toSignal(false),
      width <-- widthSignal,
      y <-- lifeSpriteHeight,
      height <-- castingBarHeight,
      tint <-- maybeCastingInfoEvents
        .collect {
          case Some((castingInfo, _)) => abilityColourMap(castingInfo.ability.abilityId)
        }
        .toSignal(RGBColour.red),
      mask := castingBarMask
    ),
    castingBarMask,
    pixiText(
      "",
      text <-- bossEvents.map(_.name).toSignal(""), // toSignal so that we don't change when not needed.
      textStyle := new TextStyle(
        Align(
          fontSize = 10.0
        )
      )
    )
  )

}
