package game.ui.components

import game.IndigoViewModel
import game.ui.*
import scala.scalajs.js
import indigo.*

final class FPSDisplay(anchor: Anchor)(using viewModel: IndigoViewModel)
    extends Container(200, 28, anchor) {

  val report     = viewModel.telemetry.fps
  val textHeight = 8
  val fontSize   = Pixels(textHeight)

  def text(content: String, offset: Point) = TextComponent(
    content,
    fontSize,
    Anchor.topLeft.withOffset(offset),
    RGBA.Black,
    width,
    textHeight
  )

  override def children: js.Array[Component] = js.Array(
    text(s"Avg: ${report.average}", Point.zero),
    text(s"Min: ${report.min}", Point(0, textHeight + 2)),
    text(s"Max: ${report.max}", Point(0, 2 * (textHeight + 2)))
  )

}
