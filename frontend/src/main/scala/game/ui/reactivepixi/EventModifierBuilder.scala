package game.ui.reactivepixi

import com.raquo.airstream.core.Observer
import game.ui.reactivepixi.EventModifierBuilder.ReactiveInteractionEvent
import game.ui.reactivepixi.ReactivePixiElement.Base
import typings.pixiJs.PIXI.interaction.{InteractionEvent, InteractionEventTypes}

import scala.scalajs.js

trait EventModifierBuilder[-El <: ReactivePixiElement.Base, T] {

  def map[U](f: T => U): EventModifierBuilder[El, U] = {
    val thisEMB = this
    (observer: Observer[U]) => thisEMB --> observer.contramap(f)
  }
  def mapTo[U](u: => U): EventModifierBuilder[El, U] = map(_ => u)

  def -->(observer: Observer[T]): PixiModifier[El]

  def stopPropagation()(implicit ev: T <:< ReactiveInteractionEvent[_]): EventModifierBuilder[El, T] =
    map { event =>
      ev(event).event.stopPropagation()
      event
    }

}

object EventModifierBuilder {

  case class ReactiveInteractionEvent[El <: ReactivePixiElement.Base](
      element: El,
      event: InteractionEvent
  )

  val onClick: EventModifierBuilder[Base, ReactiveInteractionEvent[Base]] = onClickFor[Base]

  def onClickFor[El <: Base]: EventModifierBuilder[El, ReactiveInteractionEvent[El]] =
    new EventModifierBuilder[El, ReactiveInteractionEvent[El]] {
      def -->(observer: Observer[ReactiveInteractionEvent[El]]): PixiModifier[El] = new PixiModifier[El] {
        def apply(element: El): Unit = {

          val listener: js.Function1[InteractionEvent, Unit] = { event =>
            observer.onNext(ReactiveInteractionEvent(element, event))
          }

          element.ref.addListener(InteractionEventTypes.click, listener)
        }
      }
    }

}
