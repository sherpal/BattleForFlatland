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
import gamelogic.abilities.Ability
import game.gameutils.toIndigo
import assets.fonts.Fonts.AllowedColor

final case class BossCooldownsContainer()(using viewModel: IndigoViewModel) extends Component {

  def alpha = 1.0

  val maybeBoss = viewModel.gameState.bosses.values.headOption

  override def width: Int = 150

  override val children: js.Array[Component] = maybeBoss match {
    case None => js.Array()
    case Some(boss) =>
      js.Array(
        GridContainer(
          GridContainer.Column,
          20,
          boss.abilityNames.toJSArray.map { (abilityId, name) =>
            val colour                   = Ability.abilityColour(abilityId)
            val textColour: AllowedColor = if colour.isBright then "black" else "white"
            val maybeLastUse             = boss.relevantUsedAbilities.get(abilityId)
            val remainingTime = maybeLastUse.fold(0L) { lastUse =>
              val elapsedTime = viewModel.gameState.time - lastUse.time
              val cooldown    = lastUse.cooldown
              (cooldown - elapsedTime) max 0L
            }
            val value = remainingTime / maybeLastUse.fold(1L)(_.cooldown).toDouble
            new Container(width, 15, Anchor.topLeft) {
              def children = js.Array(
                StatusBar(
                  value,
                  1.0,
                  _ => colour.toIndigo,
                  Asset.ingame.gui.bars.minimalist,
                  StatusBar.Horizontal,
                  this.width,
                  this.height,
                  Anchor.topLeft
                ),
                TextComponent(
                  name,
                  Anchor.left,
                  this.width * 8 / 10,
                  this.height,
                  textColour,
                  Fonts.m
                ),
                TextComponent(
                  if remainingTime >= 1000 then (remainingTime / 1000).toString
                  else s".${remainingTime / 100}",
                  Anchor.left.withOffset(Point(-2, 0)),
                  this.width,
                  this.height,
                  textColour,
                  Fonts.m,
                  textAlign = TextAlignment.Right
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

  override def present(bounds: Rectangle, alpha: Double): js.Array[SceneNode] = js.Array()

  override def visible: Boolean = maybeBoss.isDefined

  override def registerEvents(bounds: Rectangle): js.Array[EventRegistration[?]] = js.Array()

  override def anchor: Anchor = Anchor.topRight

}
