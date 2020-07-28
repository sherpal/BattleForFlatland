package game.ui.effects.test

import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.Signal
import game.Camera
import gamelogic.physics.pathfinding.Graph
import game.ui.reactivepixi.ReactivePixiElement.{pixiGraphics, ReactiveGraphics}
import game.ui.reactivepixi.AttributeModifierBuilder.moveGraphics
import gamelogic.physics.Complex
import typings.pixiJs.PIXI.Graphics

final class GraphDrawer(graphSignal: EventStream[Graph], camera: Camera) {

  val graphics: ReactiveGraphics = pixiGraphics(
    moveGraphics <-- graphSignal.map(_.allEdges).map { edges =>
      {

        { graphics: Graphics =>
          graphics.clear().lineStyle(3).beginFill(0xFF0000)
          edges.map { case (from, to) => (camera.worldToLocal(from), camera.worldToLocal(to)) }.foreach {
            case (from, to) =>
              graphics.moveTo(from.re, from.im).lineTo(to.re, to.im)
          }
          graphics.endFill()
        }
      }
    }
  )

}
