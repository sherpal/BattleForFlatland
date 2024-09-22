package game.ui.components.buffcontainers

import gamelogic.entities.Entity
import gamelogic.buffs.Buff
import game.scenes.ingame.InGameScene
import game.IndigoViewModel
import game.ui.*
import indigo.*
import game.scenes.ingame.InGameScene.StartupData
import assets.Asset
import scala.scalajs.js
import game.events.CustomIndigoEvents
import game.ui.components.StatusBar

final case class BuffIcon(
    entityId: Entity.Id,
    buff: Buff,
    iconSize: Int,
    offset: Point = Point.zero
)(using context: FrameContext[StartupData], viewModel: IndigoViewModel)
    extends Component {
  val buffAsset = Asset.buffAssetMap(buff.resourceIdentifier)

  override val children: js.Array[Component] =
    if buff.isFinite then
      js.Array(
        StatusBar(
          buff.remainingPercentage(viewModel.gameState.time),
          1.0,
          _ => RGBA.Black.withAlpha(0.5),
          Asset.ingame.gui.abilities.abilityOverlay,
          StatusBar.Vertical,
          iconSize,
          iconSize,
          Anchor.topLeft
        )
      )
    else js.Array()

  override def width: Int = iconSize

  override def height: Int = iconSize

  override def visible: Boolean = true

  override def present(bounds: Rectangle): js.Array[SceneNode] =
    js.Array(
      buffAsset
        .indigoGraphic(
          bounds.center,
          None,
          Radians.zero,
          bounds.size
        )
        .withDepth(Depth(4))
    )

  override def registerEvents(parentRectangle: Rectangle) = js.Array()

  override def anchor: Anchor = Anchor.topLeft.withOffset(offset)

}
