package game.ui.components.buffcontainers

import game.ui.*
import indigo.*

import scala.scalajs.js
import game.scenes.ingame.InGameScene
import game.IndigoViewModel
import game.scenes.ingame.InGameScene.StartupData
import gamelogic.entities.Entity
import game.events.CustomIndigoEvents

import scala.scalajs.js.JSConverters.*
import gamelogic.buffs.Buff
import game.ui.components.grid.GridContainer

case class BuffContainer(
    entityId: Entity.Id,
    anchor: Anchor
)(using context: FrameContext[StartupData], viewModel: IndigoViewModel)
    extends Component {

  val children: js.Array[Component] =
    val buffs = viewModel.gameState.allBuffsOfEntity(entityId).toJSArray
    buffs
      .sortBy(_.appearanceTime)
      .zipWithIndex
      .map((buff, index) =>
        BuffIcon(
          entityId,
          buff,
          20,
          Point(index * 20, 0)
        )
      )

  override val height: Int = children.headOption.fold(0)(_.height)

  override val width: Int = children.headOption.fold(0)(_.width)

  override def present(bounds: Rectangle): scala.scalajs.js.Array[SceneNode] = js.Array()

  override def registerEvents(parentRectangle: Rectangle) = js.Array()

  override def visible: Boolean =
    true

}
