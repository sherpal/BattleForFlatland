package game.ui.gui.reactivecomponents

import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.Signal
import game.ui.reactivepixi.AttributeModifierBuilder._
import game.ui.reactivepixi.ReactivePixiElement.{pixiContainer, pixiGraphics, pixiSprite, ReactiveContainer}
import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.mod.Container
import utils.misc.RGBColour

final class CastingBar(
    entityId: Entity.Id,
    uiContainer: ReactiveContainer,
    positions: Signal[Complex],
    dimensions: Signal[(Double, Double)],
    frameTexture: Texture,
    innerTexture: Texture,
    updateStream: EventStream[(GameState, Long)],
    abilityIdColourMap: Map[Ability.AbilityId, RGBColour]
) extends GUIComponent {

  private val maybeCastingInfoStream = updateStream.map {
    case (gameState, time) =>
      gameState.castingEntityInfo.get(entityId).map((_, time))
  }

  private val innerGraphicsDrawingEvents = maybeCastingInfoStream.withCurrentValueOf(dimensions).collect {
    case (Some((castingInfo, currentTime)), (width, height)) =>
      val currentCastTime    = (currentTime - castingInfo.startedTime).toDouble
      val castingProgression = currentCastTime / castingInfo.ability.castingTime

      val redrawGraphics: typings.pixiJs.PIXI.Graphics => Unit =
        _.clear()
          .beginFill(0xc0c0c0)
          .drawRect(0, 0, width * castingProgression, height)
      redrawGraphics
  }

  val frameSprite = pixiSprite(frameTexture, dims <-- dimensions)

  val maskG = pixiGraphics(moveGraphics <-- innerGraphicsDrawingEvents)

  val innerSprite = pixiSprite(
    innerTexture,
    tint <-- maybeCastingInfoStream.collect { case Some((info, _)) => abilityIdColourMap(info.ability.abilityId) },
    dims <-- dimensions,
    mask := maskG
  )

  container.amend(
    position <-- positions,
    visible <-- maybeCastingInfoStream.map(_.isDefined).toSignal(true),
    innerSprite,
    frameSprite,
    maskG
  )

  uiContainer.amend(container)

}
