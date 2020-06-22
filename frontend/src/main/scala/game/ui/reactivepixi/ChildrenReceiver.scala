package game.ui.reactivepixi

import com.raquo.airstream.core.Observable
import game.ui.reactivepixi.ReactivePixiElement.ReactiveContainer

object ChildrenReceiver {

  def <--(
      childrenObs: Observable[List[ReactivePixiElement.Base]]
  ): PixiModifier[ReactivePixiElement.ReactiveContainer] =
    new PixiModifier[ReactiveContainer] {
      def apply(element: ReactiveContainer): Unit =
        childrenObs.foreach { children =>
          // todo: probably optimize this
          val (remaining, leaving) = element.children.partition(children.contains)
          element.children = remaining
          leaving.foreach(_.destroy())
          val newChildren = children.filterNot(remaining.contains)
          newChildren.foreach(ReactivePixiElement.addChildTo(element, _))
        } {
          element
        }
    }

  def children: ChildrenReceiver.type = this

}
