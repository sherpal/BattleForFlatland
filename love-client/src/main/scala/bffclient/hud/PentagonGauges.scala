package bffclient.hud

import bffclient.gui.*
import bffclient.render.Painter
import bffclient.ui.Colour as Paint
import gamelogic.buffs.abilities.classes.Facets
import gamelogic.entities.Entity
import gamelogic.entities.classes.Pentagon
import gamelogic.entities.classes.pentagon.SteadyAim

/** Our lit [[Facets]], when we are a [[Pentagon]], to be put on the left of our frame. Once they
  * are all lit, Pentagram is ready.
  */
final class FacetsGauge(parent: Frame) {
  private val gauge = StacksGauge(parent, Facets.maxStacks.toInt, StacksGauge.narrowWidth)
  val frame: Frame  = gauge.frame

  /** Shows the facets of `id`, if it is a living [[Pentagon]]. */
  def update(p: Painter, id: Option[Entity.Id]): Unit = {
    val pentagon =
      id.flatMap(p.gameState.players.get).collect { case pentagon: Pentagon => pentagon }
    gauge.show(
      pentagon.map(pentagon => Facets.stacksOn(p.gameState, pentagon.id).fold(0)(_.toInt)),
      FacetsGauge.colour,
      FacetsGauge.fullColour
    )
  }
}

object FacetsGauge {
  val colour     = Paint.rgb(235, 160, 60)
  val fullColour = Paint.rgb(255, 225, 120)
}

/** The stacks of our [[SteadyAim]], when we are a [[Pentagon]], to be put on the left of the
  * [[FacetsGauge]].
  */
final class SteadyAimGauge(parent: Frame) {
  private val gauge = StacksGauge(parent, SteadyAim.maxStacks, StacksGauge.narrowWidth)
  val frame: Frame  = gauge.frame

  /** Shows the steady aim of `id`, if it is a living [[Pentagon]]. */
  def update(p: Painter, id: Option[Entity.Id]): Unit = {
    val pentagon =
      id.flatMap(p.gameState.players.get).collect { case pentagon: Pentagon => pentagon }
    gauge.show(
      pentagon.map(SteadyAim.stacks(_, p.now)),
      SteadyAimGauge.colour,
      SteadyAimGauge.fullColour
    )
  }
}

object SteadyAimGauge {
  val colour     = Paint.rgb(80, 190, 150)
  val fullColour = Paint.rgb(140, 240, 200)
}
