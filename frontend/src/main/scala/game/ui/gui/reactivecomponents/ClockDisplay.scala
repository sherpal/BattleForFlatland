package game.ui.gui.reactivecomponents

import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.Signal
import game.ui.reactivepixi.AttributeModifierBuilder._
import game.ui.reactivepixi.ReactivePixiElement.pixiText
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import typings.pixiJs.anon.Align
import typings.pixiJs.mod.TextStyle

/**
  * Displays the time since the beginning of the game (in minutes:seconds) at the given position.
  *
  * @param updates stream of game state with current time. The clock is re-computed each time the stream emits. That
  *                means that it's useless to give a stream emitting more than once per second.
  * @param positions signal with the (top left) position of the container in the canvas.
  */
final class ClockDisplay(updates: EventStream[(GameState, Long)], positions: Signal[Complex]) extends GUIComponent {

  def displayTime(gs: GameState, currentTime: Long): String = gs.startTime.fold("0:00") { startTime =>
    val gameTime = gs.endTime.getOrElse(currentTime)

    val elapsedSeconds = (gameTime - startTime) / 1000 // game time is in milliseconds
    val seconds        = elapsedSeconds % 60
    val minutes        = elapsedSeconds / 60

    s"$minutes:${"%02d" format seconds}"
  }

  container.amend(
    position <-- positions,
    pixiText(
      "",
      text <-- updates.map((displayTime _).tupled),
      textStyle := new TextStyle(
        Align().setFontSize(15)
      )
    )
  )

}
