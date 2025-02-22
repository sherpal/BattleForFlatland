package game.ui.components

import game.ui.*
import indigo.*
import scala.scalajs.js
import game.IndigoViewModel
import game.ui.Component.EventRegistration
import game.ui.components.grid.GridContainer
import scala.scalajs.js.JSConverters.*
import assets.Asset
import utils.misc.RGBColour
import assets.fonts.Fonts

final case class BossThreadMeter()(using viewModel: IndigoViewModel) extends Component {

  override def alpha = 1.0

  val maybeBoss = viewModel.gameState.bosses.values.headOption

  val damageThreats = maybeBoss
    .map(_.damageThreats.toJSArray)
    .getOrElse(js.Array())
    .map { (id, amount) =>
      viewModel.gameState.players.get(id) -> amount
    }
    .collect { case (Some(player), amount) =>
      player -> amount
    }
    .sortBy(-_._2)

  val maxThreatValue = damageThreats.headOption
    .map(_._2)
    .filter(_ > 0)
    .getOrElse(1.0)

  override def width: Int = 150

  override val children: js.Array[Component] = js.Array(
    TextComponent("Threat Meter", Anchor.topLeft, width, 18, "black", Fonts.m),
    GridContainer(
      GridContainer.Column,
      20,
      damageThreats.map { (player, amount) =>
        val colour = RGBColour.fromIntColour(player.colour)
        new Container(width, 15) {
          val children = js.Array(
            StatusBar(
              amount,
              maxThreatValue,
              _ => RGBA.fromColorInts(colour.red, colour.green, colour.blue),
              Asset.ingame.gui.bars.minimalist,
              StatusBar.Horizontal,
              this.width,
              this.height,
              Anchor.left
            ),
            TextComponent(
              player.name,
              Anchor.left,
              this.width,
              this.height,
              if colour.isBright then "black" else "white",
              Fonts.m
            )
          )
        }
      },
      Anchor.bottom
    )
  )

  override def height: Int = children.map(_.height).sum

  override def present(bounds: Rectangle, alpha: Double): js.Array[SceneNode] = js.Array()

  override def visible: Boolean = true

  override def registerEvents(bounds: Rectangle): js.Array[EventRegistration[?]] = js.Array()

  override def anchor: Anchor = Anchor.bottomRight

}
