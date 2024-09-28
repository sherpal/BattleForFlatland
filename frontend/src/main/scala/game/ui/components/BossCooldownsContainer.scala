package game.ui.components

import game.ui.*
import indigo.*
import scala.scalajs.js
import game.ui.Component.EventRegistration
import gamelogic.entities.Entity
import game.IndigoViewModel
import game.ui.components.grid.GridContainer
import utils.misc.RGBColour
import assets.Asset
import scala.scalajs.js.JSConverters.*
import assets.fonts.Fonts

final case class BossCooldownsContainer()(using viewModel: IndigoViewModel) extends Component {

  val maybeBoss = viewModel.gameState.bosses.values.headOption

  override def width: Int = 150

  override val children: js.Array[Component] = maybeBoss match {
    case None => js.Array()
    case Some(boss) =>
      js.Array(
        GridContainer(
          GridContainer.Column,
          20,
          boss.abilityNames.zip(RGBColour.repeatedColours).toJSArray.map {
            case ((abilityId, name), colour) =>
              val maybeLastUse = boss.relevantUsedAbilities.get(abilityId)
              val value = maybeLastUse.fold(0.0) { lastUse =>
                val elapsedTime = viewModel.gameState.time - lastUse.time
                val cooldown    = lastUse.cooldown
                (cooldown - elapsedTime) / cooldown.toDouble
              }
              new Container(width, 15, Anchor.topLeft) {
                def children = js.Array(
                  StatusBar(
                    value,
                    1.0,
                    _ => RGBA.fromColorInts(colour.red, colour.green, colour.blue),
                    Asset.ingame.gui.bars.minimalist,
                    StatusBar.Horizontal,
                    this.width,
                    this.height,
                    Anchor.topLeft
                  ),
                  TextComponent(
                    name,
                    Anchor.left,
                    this.width,
                    this.height,
                    if colour.isBright then "black" else "white",
                    Fonts.m
                  )
                )
              }
          },
          Anchor.topRight,
          true
        )
      )
  }

  override def height: Int = children.map(_.height).sum

  override def present(bounds: Rectangle): js.Array[SceneNode] = js.Array()

  override def visible: Boolean = maybeBoss.isDefined

  override def registerEvents(bounds: Rectangle): js.Array[EventRegistration[?]] = js.Array()

  override def anchor: Anchor = Anchor.topRight

}