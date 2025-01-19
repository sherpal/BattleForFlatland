package game.ui.components

import gamelogic.entities.Entity
import game.ui.*
import indigo.*
import scala.scalajs.js
import game.ui.Component.EventRegistration
import game.IndigoViewModel
import assets.Asset
import game.gameutils.*

final case class PlayerCastBar(playerId: Entity.Id)(using viewModel: IndigoViewModel)
    extends Container(150, 15, Anchor.bottom.withOffset(Point(0, -35))) {

  val maybeCastInfo = viewModel.gameState.castingEntityInfo.get(playerId)

  override val visible: Boolean = maybeCastInfo.isDefined

  override def children: js.Array[Component] = maybeCastInfo match {
    case None => js.Array()
    case Some(castingInfo) =>
      js.Array(
        StatusBar(
          1.0,
          1.0,
          _ => RGBA.fromColorInts(128, 128, 128),
          Asset.ingame.gui.bars.minimalist,
          StatusBar.Horizontal,
          this.width,
          this.height,
          Anchor.topLeft
        ),
        StatusBar(
          viewModel.gameState.time - castingInfo.startedTime.toDouble,
          castingInfo.castingTime.toDouble,
          _ => castingInfo.ability.abilityColour.toIndigo,
          Asset.ingame.gui.bars.minimalist,
          StatusBar.Horizontal,
          this.width,
          this.height,
          Anchor.topLeft
        )
      )
  }

}
