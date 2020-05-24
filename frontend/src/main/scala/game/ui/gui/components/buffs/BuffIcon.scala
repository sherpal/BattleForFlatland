package game.ui.gui.components.buffs

import game.ui.gui.components.{GUIComponent, StatusBar}
import gamelogic.buffs.Buff
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.mod.Sprite
import utils.misc.RGBColour

final class BuffIcon(val entityId: Entity.Id, val buffId: Buff.Id, texture: Texture) extends GUIComponent {

  private val sprite = new Sprite(texture)
  sprite.width  = 30
  sprite.height = 30
  container.addChild(sprite)

  private val countdown = new StatusBar(
    { (gameState, currentTime) =>
      (for {
        buff <- gameState.buffById(entityId, buffId)
        startTime = buff.appearanceTime
        elapsed   = currentTime - startTime
        duration  = buff.duration
      } yield 1 - elapsed / duration.toDouble).getOrElse(0.0)
    }, { (_, _) =>
      RGBColour.black.withAlpha(0.5)
    }, // color
    { (gameState, _) =>
      gameState.buffById(entityId, buffId).isDefined
    }, //visible
    texture,
    StatusBar.Vertical
  )
  container.addChild(countdown.container)
  countdown.setSize(30.0, 30.0)

  def update(gameState: GameState, currentTime: Long): Unit =
    countdown.update(gameState, currentTime)

}

object BuffIcon {

  implicit final val ordering: Ordering[BuffIcon] = Ordering.by(_.buffId)

}
