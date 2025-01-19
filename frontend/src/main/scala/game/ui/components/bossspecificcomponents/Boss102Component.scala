package game.ui.components.bossspecificcomponents

import game.IndigoViewModel
import indigo.*
import game.scenes.ingame.InGameScene.StartupData
import game.ui.*
import game.ui.Component.EventRegistration

import scala.scalajs.js
import gamelogic.entities.boss.boss102.BossHound

class Boss102Component()(using viewModel: IndigoViewModel, context: FrameContext[StartupData])
    extends Component {

  override def alpha: Double = 1.0

  override def height: Int = context.startUpData.bounds.height

  override def width: Int = context.startUpData.bounds.width

  override def anchor: Anchor = Anchor.topLeft

  val numberOfHounds = viewModel.gameState.allTEntities[BossHound].size

  override def children: js.Array[Component] = js.Array(
    TextComponent(
      s"Hounds: $numberOfHounds",
      Anchor.right,
      200,
      16,
      "black",
      16,
      TextAlignment.Right,
      true
    )
  )

  override def registerEvents(bounds: Rectangle): js.Array[EventRegistration[?]] = js.Array()

  override def visible: Boolean = true

  override def present(bounds: Rectangle, alpha: Double): js.Array[SceneNode] = js.Array()

}
