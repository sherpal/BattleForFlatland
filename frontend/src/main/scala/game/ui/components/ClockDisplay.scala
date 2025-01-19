package game.ui.components

import indigo.*
import game.ui.*

import scala.scalajs.js
import game.IndigoViewModel

class ClockDisplay()(using viewModel: IndigoViewModel) extends Container(200, 16, Anchor.topLeft) {

  val text =
    viewModel.gameState.startTime.fold("Not Started") { startTime =>
      val currentTime     = viewModel.gameState.endTime.getOrElse(viewModel.gameState.time)
      val ellapsedSeconds = (currentTime - startTime) / 1000
      def minutes         = ellapsedSeconds / 60
      def seconds         = ellapsedSeconds % 60
      s"$minutes:${"%02d".format(seconds)}"
    }

  val children: js.Array[Component] = js.Array(
    TextComponent(text, Anchor.topLeft, width, height, "black", 16)
  )

}
