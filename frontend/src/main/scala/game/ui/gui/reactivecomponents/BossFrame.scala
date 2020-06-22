package game.ui.gui.reactivecomponents

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventstream.EventStream
import gamelogic.entities.{Entity, EntityCastingInfo}
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.Texture
import game.ui.reactivepixi.ReactivePixiElement._
import game.ui.reactivepixi.AttributeModifierBuilder._
import game.ui.reactivepixi.EventModifierBuilder._
import gamelogic.entities.boss.BossEntity
import typings.pixiJs.anon.Align
import typings.pixiJs.mod.TextStyle
import utils.misc.RGBColour

final class BossFrame(
    val entityId: Entity.Id,
    backgroundTexture: Texture,
    lifeTexture: Texture,
    castingBarTexture: Texture,
    targetFromGUIWriter: Observer[Entity.Id],
    gameStateUpdates: EventStream[(GameState, Long)]
) extends GUIComponent {

  val maybeBossEvents: EventStream[Option[BossEntity]] = gameStateUpdates.map(_._1).map(_.bosses.get(entityId))
  val bossEvents: EventStream[BossEntity]              = maybeBossEvents.collect { case Some(boss) => boss }

  val maybeCastingInfoEvents: EventStream[Option[(EntityCastingInfo, Long)]] =
    gameStateUpdates.map { case (gameState, time) => gameState.castingEntityInfo.get(entityId).map((_, time)) }

  val componentWidth  = 200.0
  val componentHeight = 15.0

  val lifeSpriteHeight: Double = componentHeight * 2 / 3
  val castingBarHeight: Double = componentHeight - lifeSpriteHeight

  val lifeMask: ReactiveGraphics = pixiGraphics(
    moveGraphics <-- bossEvents.map { boss =>
      _.clear().beginFill(0xc0c0c0).drawRect(0, 0, componentWidth * boss.life / boss.maxLife, lifeSpriteHeight)
    }
  ) // lifeMask

  val castingBarMask: ReactiveGraphics = pixiGraphics( // castingBarMask
    moveGraphics <-- maybeCastingInfoEvents.collect {
      case Some((castingInfo, currentTime)) =>
        val currentCastTime    = (currentTime - castingInfo.startedTime).toDouble
        val castingProgression = currentCastTime / castingInfo.ability.castingTime
        _.clear().beginFill(0xc0c0c0).drawRect(0, 0, componentWidth * castingProgression, castingBarHeight)
    },
    y := lifeSpriteHeight
  )

  container.amend(
    interactive := true,
    onClick.stopPropagation().mapTo(entityId) --> targetFromGUIWriter,
    pixiSprite( // backgroundSprite
      backgroundTexture,
      width := componentWidth,
      height := componentHeight
    ),
    pixiSprite( // lifeSprite
      lifeTexture,
      width := componentWidth,
      height := lifeSpriteHeight,
      tint := RGBColour.green,
      mask := lifeMask
    ),
    lifeMask,
    pixiSprite( // castingBar
      castingBarTexture,
      visible <-- maybeCastingInfoEvents.map(_.isDefined).toSignal(false),
      width := componentWidth,
      y := lifeSpriteHeight,
      height := castingBarHeight,
      tint := RGBColour.red,
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
