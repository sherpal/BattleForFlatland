package game.ui.gui.reactivecomponents

import com.raquo.airstream.eventstream.EventStream

import scala.collection.immutable.Queue
import game.ui.reactivepixi.ReactivePixiElement._
import game.ui.reactivepixi.AttributeModifierBuilder._
import typings.pixiJs.anon.Align
import typings.pixiJs.mod.TextStyle

/**
  * Small pixi component for displaying the frame rate, computed over 100 ticks.
  */
final class FPSDisplay(ticks: EventStream[Unit]) extends GUIComponent {

  val fps: EventStream[Long] = ticks
    .mapTo(System.currentTimeMillis())
    .fold(Queue[Long](System.currentTimeMillis())) { (queue, newTime) =>
      val newQueue = queue.enqueue(newTime)
      if (newQueue.size > 100) newQueue.dequeue._2
      else newQueue
    }
    .changes
    .map(queue => 1000 * queue.size.toDouble / (queue.last - queue.head))
    .map(math.round)

  container.amend(
    pixiText(
      "",
      text <-- fps.map(_.toString).toSignal(""),
      textStyle := new TextStyle(
        Align(
          fontSize = 30.0
        )
      ),
      x := 10,
      y := 10
    )
  )

}
