package game.ui.gui.components

import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.mod.{Container, Graphics, Sprite}
import game.ui.reactivepixi.ReactivePixiElement._
import game.ui.reactivepixi.AttributeModifierBuilder._
import gamelogic.abilities.Ability
import utils.misc.RGBColour

final class CastingBar(
    entityId: Entity.Id,
    uiContainer: Container,
    frameTexture: Texture,
    innerTexture: Texture,
    updateStream: EventStream[(GameState, Long)],
    abilityIdColourMap: Map[Ability.AbilityId, RGBColour]
) extends GUIComponent {

  val barWidth  = 200.0
  val barHeight = 15.0

  private val maybeCastingInfoStream = updateStream.map {
    case (gameState, time) =>
      gameState.castingEntityInfo.get(entityId).map((_, time))
  }

  private val innerGraphicsDrawingEvents = maybeCastingInfoStream.collect {
    case Some((castingInfo, currentTime)) =>
      val currentCastTime    = (currentTime - castingInfo.startedTime).toDouble
      val castingProgression = currentCastTime / castingInfo.ability.castingTime

      val redrawGraphics: typings.pixiJs.PIXI.Graphics => Unit =
        _.clear()
          .beginFill(0xc0c0c0)
          .drawRect(0, 0, barWidth * castingProgression, barHeight)
      redrawGraphics
  }

  val frameSprite = pixiSprite(frameTexture, width := barWidth, height := barHeight)

  val maskG = pixiGraphics(moveGraphics <-- innerGraphicsDrawingEvents)

  val innerSprite = pixiSprite(
    innerTexture,
    tint <-- maybeCastingInfoStream.collect { case Some((info, _)) => abilityIdColourMap(info.ability.abilityId) },
    width := barWidth,
    height := barHeight,
    mask := maskG
  )

  val ctnr: ReactiveContainer = pixiContainer(
    visible <-- maybeCastingInfoStream.map(_.isDefined).startWith(true),
    innerSprite,
    frameSprite,
    maskG
  )

  uiContainer.addChild(container)
  container.addChild(ctnr.ref)

  def update(gameState: GameState, currentTime: Long): Unit = ()

}
