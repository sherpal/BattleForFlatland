package game.drawers

import gamelogic.entities.{Body, LivingEntity, WithAbilities}
import game.ui.Container
import indigo.*
import game.gameutils.toIndigo

import scala.scalajs.js
import game.ui.Anchor
import game.ui.Component
import game.ui.components.StatusBar
import assets.Asset
import gamelogic.gamestate.GameState

def minicastingbar(
    entity: Body & WithAbilities,
    barPos: Point,
    gameState: GameState
): Container =
  new Container((entity.shape.radius * 2).toInt, 5, Anchor.center.withOffset(barPos)) {
    def children: js.Array[Component] =
      gameState.castingEntityInfo.get(entity.id).filter(_.isCasting(gameState.time, 0L)) match {
        case Some(castingInfo) =>
          js.Array[Component](
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
              (castingInfo.castingTime - castingInfo
                .remainingCastingTime(gameState.time, 0L)).toDouble,
              castingInfo.castingTime.toDouble,
              _ => castingInfo.ability.abilityColour.asRGBAColour.toIndigo,
              Asset.ingame.gui.bars.minimalist,
              StatusBar.Horizontal,
              width,
              height,
              Anchor.topLeft
            )
          )
        case None => js.Array[Component]()
      }
  }

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
