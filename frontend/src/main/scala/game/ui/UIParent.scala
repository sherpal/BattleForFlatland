package game.ui

import indigo.*

import scala.scalajs.js
import game.events.CustomIndigoEvents
import game.ui.Component.EventRegistration

final case class UIParent[StartupData, ViewModel](
    children: (FrameContext[StartupData], ViewModel) => js.Array[Component],
    theWidth: Int,
    theHeight: Int,
    lastSlowFrameTick: Seconds = Seconds.zero
) {

  private inline def slowFrameTickRate = Seconds(0.5)

  def visible(context: FrameContext[StartupData], viewModel: ViewModel): Boolean = true

  def rectangle: Rectangle = Rectangle(0, 0, theWidth, theHeight)

  def registerEvents(
      context: FrameContext[StartupData],
      viewModel: ViewModel,
      bounds: Rectangle
  ): scala.scalajs.js.Array[EventRegistration[?]] = js.Array(
    EventRegistration[FrameTick](_ =>
      if context.gameTime.running - lastSlowFrameTick > slowFrameTickRate then
        js.Array(CustomIndigoEvents.UIEvent.SlowFrameTick())
      else js.Array()
    )
  )

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
      update: (ViewModel, UIParent[StartupData, ViewModel]) => ViewModel,
      updateCachedComponents: (
          ViewModel,
          Component.CachedComponentsInfo
      ) => ViewModel,
      retrieveComponentCache: ViewModel => Component.CachedComponentsInfo,
      event: GlobalEvent
  ): Outcome[ViewModel] = {
    val newUIParent = event match {
      case CustomIndigoEvents.UIEvent.SlowFrameTick() =>
        copy(lastSlowFrameTick = context.gameTime.running)
      case _ => this
    }

    event match {
      case FrameTick =>
        // refresh the component cache
        val cache = generateCachedComponents(context, viewModel)
        Outcome(
          updateCachedComponents(update(viewModel, newUIParent), cache)
        ).addGlobalEvents(Batch(cache.handleEvent(FrameTick)))
      case other =>
        Outcome(update(viewModel, newUIParent)).addGlobalEvents(
          Batch(retrieveComponentCache(viewModel).handleEvent(other))
        )
    }
    // val allComponents = newUIParent.allDescendants(context, viewModel, rectangle)

    // val generatedEvents =
    //   allComponents.flatMap(_.registerEvents(context, viewModel)).flatMap(_.handle(event))
    // Outcome(update(viewModel, newUIParent)).addGlobalEvents(Batch(generatedEvents))
  }

}
