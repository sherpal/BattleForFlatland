package game.ui.gui.reactivecomponents.buffcontainer

import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.{Signal, Val}
import game.ui.gui.reactivecomponents.{GUIComponent, StatusBar}
import gamelogic.buffs.Buff
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import typings.pixiJs.PIXI.Texture
import game.ui.reactivepixi.ReactivePixiElement._
import game.ui.reactivepixi.AttributeModifierBuilder._
import utils.misc.RGBColour

final class BuffIcon(
    val entityId: Entity.Id,
    val buffId: Buff.Id,
    texture: Texture,
    gameStateUpdates: EventStream[(GameState, Long)],
    dimensions: Signal[(Double, Double)]
) extends GUIComponent {

  private val maybeBuffEvents = gameStateUpdates.map {
    case (gs, currentTime) => (gs.buffById(entityId, buffId), currentTime)
  }

  private val barFilling = maybeBuffEvents.map {
    case (maybeBuff, currentTime) =>
      (for {
        buff <- maybeBuff
        _ <- Option.when(buff.isFinite)(())
        startTime = buff.appearanceTime
        elapsed   = currentTime - startTime
        duration  = buff.duration
      } yield 1 - elapsed / duration.toDouble)
        .getOrElse(0.0)
  }

  container.amend(
    pixiSprite(
      texture,
      dims <-- dimensions
    ),
    new StatusBar(
      barFilling.toSignal(0.0),
      Val(RGBColour.black.withAlpha(0.5)),
      maybeBuffEvents.map(_._1.isDefined).toSignal(true),
      texture,
      dimensions,
      StatusBar.Vertical
    )
  )

}
