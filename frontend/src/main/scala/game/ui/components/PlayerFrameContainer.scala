package game.ui.components

import game.scenes.ingame.InGameScene
import game.IndigoViewModel
import game.ui.*
import indigo.*
import game.scenes.ingame.InGameScene.StartupData
import game.ui.components.grid.GridContainer
import game.events.CustomIndigoEvents.UIEvent.*

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import gamelogic.entities.Entity

case class PlayerFrameContainer(
    myId: Entity.Id,
    offset: Point
)(using context: FrameContext[InGameScene.StartupData], viewModel: IndigoViewModel)
    extends Component {

  def alpha: Double = 1.0

  def children: js.Array[Component] =
    js.Array(
      GridContainer(
        GridContainer.Column,
        20,
        (viewModel.gameState.players ++ viewModel.gameState.deadPlayers).toJSArray
          .sortBy(_._2.name)
          .map { (playerId, player) =>
            PlayerFrame(myId, playerId, player.cls, inGroup = true)
          },
        anchor = Anchor.topLeft
      )
    )

  override def width: Int = 150

  override val height: Int = children.map(_.height).sum

  override def registerEvents(bounds: Rectangle): js.Array[Component.EventRegistration[?]] =
    js.Array()

  override def visible: Boolean = true

  override def present(bounds: Rectangle, alpha: Double): js.Array[SceneNode] = js.Array()

  override def anchor: Anchor = Anchor.topLeft.withOffset(offset)

}
