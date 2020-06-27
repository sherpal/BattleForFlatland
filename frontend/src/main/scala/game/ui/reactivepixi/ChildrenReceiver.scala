package game.ui.reactivepixi

import com.raquo.airstream.core.Observable
import game.ui.reactivepixi.ReactivePixiElement.ReactiveContainer

object ChildrenReceiver {

  def <--(
      childrenObs: Observable[List[ReactivePixiElement.Base]]
  ): PixiModifier[ReactivePixiElement.ReactiveContainer] =
    new PixiModifier[ReactiveContainer] {

      var currentChildren: Vector[ReactivePixiElement.Base] = Vector.empty

      def apply(element: ReactiveContainer): Unit = {
        element.destroyCallbacks :+= { () =>
          currentChildren.foreach(_.destroy())
        }
        childrenObs.foreach { children =>
          // todo: probably optimize this
          val (remaining, leaving) = currentChildren.partition(children.contains)
          currentChildren = remaining
          leaving.foreach { _.destroy() }
          val newChildren = children.filterNot(remaining.contains)
          currentChildren ++= newChildren
          if (newChildren.nonEmpty) {
            println("new children: " + newChildren)
          }
          if (leaving.nonEmpty) {
            println("leaving: " + leaving)
          }
          newChildren.foreach(ReactivePixiElement.addChildTo(element, _))
          if (newChildren.nonEmpty) {}
        } {
          element
        }
      }
    }

  def children: ChildrenReceiver.type = this

}
