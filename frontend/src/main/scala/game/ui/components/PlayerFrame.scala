package game.ui.components

import game.scenes.ingame.InGameScene
import game.IndigoViewModel
import gamelogic.entities.Entity
import game.ui.Component
import indigo.GlobalEvent
import game.ui.Anchor
import indigo.shared.FrameContext
import indigo.Rectangle
import game.scenes.ingame.InGameScene.StartupData
import indigo.*
import game.ui.components.buffcontainers.BuffContainer

import scala.scalajs.js
import models.bff.outofgame.PlayerClasses
import assets.Asset
import game.events.CustomIndigoEvents
import indigo.shared.events.MouseEvent.Click

final case class PlayerFrame(
    playerId: Entity.Id,
    playerClass: PlayerClasses
)(using context: FrameContext[StartupData], viewModel: IndigoViewModel)
    extends Component {

  val maybePlayer = viewModel.gameState.players.get(playerId)

  private val theWidth  = 200
  private val theHeight = 55

  override def width: Int  = theWidth
  override def height: Int = theHeight

  override def registerEvents(bounds: Rectangle): js.Array[Component.EventRegistration[?]] =
    js.Array(
      Component.EventRegistration[Click](click =>
        if bounds.isPointWithin(click.position) then
          js.Array(CustomIndigoEvents.GameEvent.ChooseTarget(playerId))
        else js.Array()
      )
    )

  override def visible: Boolean = true

  override def present(bounds: Rectangle): js.Array[SceneNode] =
    viewModel.gameState.players.get(playerId) match {
      case None => js.Array()
      case Some(player) =>
        val playerImageAsset = Asset.playerClassAssetMap(player.cls)
        js.Array(
          Shape
            .Box(
              bounds,
              fill = Fill.Color(RGBA.fromColorInts(204, 255, 255)),
              stroke = Stroke(1, RGBA.Black)
            )
            .withDepth(Depth.far),
          playerImageAsset.indigoGraphic(
            bounds.position + Point(10),
            Some(RGBA.fromColorInts(player.rgb._1, player.rgb._2, player.rgb._3)),
            Radians.zero,
            Size(20)
          ),
          TextBox(player.name, theWidth, theHeight)
            .withFontFamily(FontFamily.cursive)
            .withColor(RGBA.White)
            .withFontSize(Pixels(16))
            .withStroke(TextStroke(RGBA.Red, Pixels(1)))
            .withPosition(bounds.position + Point(20, 0)),
          TextBox(
            player.resourceAmount.amount.toInt.toString ++ "/" ++ player.maxResourceAmount.toInt.toString,
            theWidth,
            theHeight
          )
            .withFontFamily(FontFamily.cursive)
            .withColor(RGBA.White)
            .withFontSize(Pixels(16))
            .withStroke(TextStroke(RGBA.Red, Pixels(1)))
            .withPosition(bounds.position + Point(20, 20))
        )
    }

  override def anchor: Anchor = Anchor.topLeft

  def children: js.Array[Component] =
    js.Array(
      StatusBar(
        maybePlayer.fold(0.0)(_.life),
        maybePlayer.fold(1.0)(_.maxLife),
        value => if value > 0.5 then RGBA.Green else if value > 0.2 then RGBA.Orange else RGBA.Red,
        Asset.ingame.gui.bars.lifeBarWenakari,
        StatusBar.Horizontal,
        180,
        20,
        Anchor.topRight
      ),
      StatusBar(
        maybePlayer.fold(0.0)(_.resourceAmount.amount),
        maybePlayer.fold(1.0)(_.maxResourceAmount),
        _ =>
          maybePlayer
            .fold(RGBA.White)(player =>
              val resourceColour = player.resourceType.colour
              RGBA.fromColorInts(resourceColour.red, resourceColour.green, resourceColour.blue)
            ),
        Asset.ingame.gui.bars.minimalist,
        StatusBar.Horizontal,
        180,
        15,
        Anchor.topRight.withOffset(Point(0, 20))
      ),
      BuffContainer(playerId, Anchor.topLeft.withOffset(Point(0, 35)))
    )
}
