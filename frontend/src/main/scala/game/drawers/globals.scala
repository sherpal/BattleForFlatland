package game.drawers

import gamelogic.entities.Body
import gamelogic.entities.LivingEntity
import game.ui.Container
import indigo.*

import scala.scalajs.js
import game.ui.Anchor
import game.ui.Component
import game.ui.components.StatusBar
import assets.Asset

def minilifebar(entity: Body & LivingEntity, barPos: Point): Container =
  new Container((entity.shape.radius * 2).toInt, 5, Anchor.center.withOffset(barPos)) {
    def children: js.Array[Component] = js.Array(
      StatusBar(
        1,
        1,
        _ => RGBA.fromColorInts(250, 250, 250),
        Asset.ingame.gui.bars.minimalist,
        StatusBar.Horizontal,
        width,
        height,
        Anchor.topLeft
      ),
      StatusBar(
        entity.life,
        entity.maxLife,
        StatusBar.lifeStatusColor,
        Asset.ingame.gui.bars.minimalist,
        StatusBar.Horizontal,
        width,
        height,
        Anchor.topLeft
      )
    )
  }
