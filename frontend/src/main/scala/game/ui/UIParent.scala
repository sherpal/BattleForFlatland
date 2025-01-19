package game.ui

import indigo.*

import scala.scalajs.js
import game.events.CustomIndigoEvents
import game.ui.Component.{EventRegistration, EventResult}

final case class UIParent[StartupData, ViewModel](
    children: (FrameContext[StartupData], ViewModel) => js.Array[Component],
    theWidth: Int,
    theHeight: Int
) {

  private inline def slowFrameTickRate = Seconds(0.5)

  def visible(context: FrameContext[StartupData], viewModel: ViewModel): Boolean = true

  def rectangle: Rectangle = Rectangle(0, 0, theWidth, theHeight)

  def registerEvents(
      context: FrameContext[StartupData],
      viewModel: ViewModel,
      bounds: Rectangle
  ): scala.scalajs.js.Array[EventRegistration[?]] = js.Array()

  def presentAll(context: FrameContext[StartupData], viewModel: ViewModel): js.Array[SceneNode] =
    children(context, viewModel).flatMap(_.presentWithChildren(rectangle, 1.0))

  def generateCachedComponents(
      context: FrameContext[StartupData],
      viewModel: ViewModel
  ): Component.CachedComponentsInfo = {
    val allComponents      = children(context, viewModel).flatMap(_.allDescendants(rectangle, 1.0))
    val eventRegistrations = allComponents.flatMap(_.registerEvents)

    Component.CachedComponentsInfo(
      eventRegistrations,
      allComponents
    )
  }

  def changeViewModel(
      context: FrameContext[StartupData],
      viewModel: ViewModel,
      event: GlobalEvent
  ): Outcome[(ViewModel, Boolean)] = {
    val allComponents = children(context, viewModel).flatMap(_.allDescendants(rectangle, 1.0))

    val eventResult =
      allComponents
        .flatMap(_.registerEvents)
        .map(_.handle(event))
        .reduceOption(_.combine(_))
        .getOrElse(EventResult.empty)
    Outcome((viewModel, eventResult.stopPropagation))
      .addGlobalEvents(Batch(eventResult.generatedEvents))
  }

}
