package game.ui.effects.test

import com.raquo.airstream.eventstream.EventStream
import game.Camera
import game.ui.reactivepixi.AttributeModifierBuilder.moveGraphics
import game.ui.reactivepixi.ReactivePixiElement.{pixiGraphics, ReactiveGraphics}
import gamelogic.physics.pathfinding.FiniteGraph
import typings.pixiJs.PIXI.Graphics

final class GraphDrawer(graphSignal: EventStream[FiniteGraph], camera: Camera) {

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
