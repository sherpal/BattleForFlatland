package game.ui.components.buffcontainers

import game.ui.*
import indigo.*

import scala.scalajs.js
import game.IndigoViewModel
import gamelogic.entities.Entity

import scala.scalajs.js.JSConverters.*

case class BuffContainer(
    entityId: Entity.Id,
    anchor: Anchor
)(using viewModel: IndigoViewModel)
    extends Component {

  override def alpha = 1.0

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

  override def present(bounds: Rectangle, alpha: Double): scala.scalajs.js.Array[SceneNode] =
    js.Array()

  override def registerEvents(parentRectangle: Rectangle) = js.Array()

  override def visible: Boolean =
    true

}
