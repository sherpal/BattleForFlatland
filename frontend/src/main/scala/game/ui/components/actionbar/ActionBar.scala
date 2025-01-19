package game.ui.components.actionbar

import game.ui.*
import indigo.*

import scala.scalajs.js
import game.ui.Component.EventRegistration
import gamelogic.entities.Entity
import game.scenes.ingame.InGameScene.StartupData
import game.IndigoViewModel
import scala.scalajs.js.JSConverters.*
import assets.Asset

final case class ActionBar(playerId: Entity.Id)(using
    context: FrameContext[StartupData],
    viewModel: IndigoViewModel
) extends Component {

  override def alpha = 1.0

  val maybePlayer = viewModel.gameState.players.get(playerId)

  val iconSize = 30

  override val children: js.Array[Component] = maybePlayer match {
    case None => js.Array()
    case Some(player) =>
      player.abilities.toArray.toJSArray.zipWithIndex.map { (abilityId, index) =>
        AbilityIcon(
          player,
          viewModel.lastEntityStartCastings.get(player.id).filter(_.ability.abilityId == abilityId),
          abilityId,
          now = viewModel.gameState.time,
          iconSize = iconSize,
          offset = Point(index * iconSize, 0)
        )
      }
  }

  override val height: Int = iconSize

  override val width: Int = children.map(_.width).sum

  override def present(bounds: Rectangle, alpha: Double): js.Array[SceneNode] = js.Array()

  override def visible: Boolean = true

  override def registerEvents(bounds: Rectangle): js.Array[EventRegistration[?]] = js.Array()

  override def anchor: Anchor = Anchor.bottom

}
