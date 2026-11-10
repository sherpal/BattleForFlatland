package bffclient.hud

import bffclient.gui.*
import bffclient.hud.HudWidgets.*
import bffclient.render.Painter
import bffclient.ui.Colour as Paint
import gamelogic.buffs.abilities.classes.{Facets, Fractured}
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

/** What remains of the [[Fractured]] that we put on our target, when we are a [[Pentagon]], to be
  * put on the left of the [[SteadyAimGauge]]: a gauge draining to the bottom, with the seconds left
  * above it.
  *
  * It is full for the longest a fracture can last, and its mark shows `Fractured.maxCarriedOver`:
  * below it, fracturing again loses nothing (all that remains is carried over), so the gauge turns
  * to `refreshColour`.
  */
final class FractureGauge(parent: Frame) {
  private val width   = StacksGauge.narrowWidth
  private val padding = 3.0
  private val height  = Hud.frameHeight - 2 * padding

  val frame: Frame = panel(parent, radius = 5)
  frame.setSize(width, Hud.frameHeight)

  private val track = frame.createTexture(HudStyle.trackColour, Layer.Artwork)
  place(track, frame, padding, padding, width - 2 * padding, height)
  track.setCornerRadius(2)
  private val fill = frame.createTexture(Layer.Artwork)
  fill.setCornerRadius(2)
  private val mark = frame.createTexture(Paint(1, 1, 1, 0.6), Layer.Overlay)
  place(
    mark,
    frame,
    padding - 1,
    padding + height * (1 - Fractured.maxCarriedOver.toDouble / FractureGauge.fullTime),
    width - 2 * padding + 2,
    1
  )
  private val seconds = text(frame, HudStyle.smallSize, justify = JustifyH.Center)
  seconds.setPoint(Point.Bottom, frame, Point.Top, 0, -1)
  private var filled = -1.0

  /** Shows the fracture that `id` put on `target`, if `id` is a living [[Pentagon]]. */
  def update(p: Painter, id: Option[Entity.Id], target: Option[Entity.Id]): Unit = {
    val pentagon =
      id.flatMap(p.gameState.players.get).collect { case pentagon: Pentagon => pentagon }
    frame.setShown(pentagon.isDefined)
    for pentagon <- pentagon do {
      val remaining = target
        .flatMap(Fractured.by(p.gameState, _, pentagon.id))
        .map(fractured => fractured.appearanceTime + fractured.duration - p.now)
        .filter(_ > 0)
      val ratio = remaining.fold(0.0)(ms => HudStyle.ratio(ms.toDouble, FractureGauge.fullTime))
      fill.setShown(ratio * height >= 1)
      if ratio != filled then {
        filled = ratio
        place(
          fill,
          frame,
          padding,
          padding + height * (1 - ratio),
          width - 2 * padding,
          height * ratio
        )
      }
      fill.setColour(
        if remaining.exists(_ <= Fractured.maxCarriedOver) then FractureGauge.refreshColour
        else FractureGauge.colour
      )
      seconds.setText(remaining.fold("")(ms => math.ceil(ms / 1000.0).toInt.toString))
    }
  }
}

object FractureGauge {

  /** The longest a fracture lasts (refreshed with the most carried over). */
  val fullTime: Double = (Fractured.duration + Fractured.maxCarriedOver).toDouble

  val colour        = Paint.rgb(185, 110, 220)
  val refreshColour = Paint.rgb(240, 90, 90)
}
