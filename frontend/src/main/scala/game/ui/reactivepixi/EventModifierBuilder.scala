package game.ui.reactivepixi

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.Signal
import game.ui.reactivepixi.EventModifierBuilder.ReactiveInteractionEvent
import game.ui.reactivepixi.ReactivePixiElement.Base
import typings.pixiJs.PIXI.interaction.{InteractionEvent, InteractionEventTypes}

import scala.scalajs.js

trait EventModifierBuilder[-El <: ReactivePixiElement.Base, T] {

  def map[U](f: T => U): EventModifierBuilder[El, U] =
    EventModifierBuilder.factory { (observer: Observer[U]) =>
      this --> observer.contramap(f)
    }

  def mapTo[U](u: => U): EventModifierBuilder[El, U] = map(_ => u)

  def -->(observer: Observer[T]): PixiModifier[El]

  def stopPropagation(implicit ev: T <:< ReactiveInteractionEvent[_]): EventModifierBuilder[El, T] =
    map { event =>
      ev(event).event.stopPropagation()
      event
    }

  def withCurrentValueOf[U](signal: Signal[U]): EventModifierBuilder[El, (T, U)] = {
    val outerThis = this
    EventModifierBuilder.factory { (observer: Observer[(T, U)]) =>
      new PixiModifier[El] {
        def apply(element: El): Unit = {
          val strictSignal = signal.observe(element)
          (outerThis --> observer.contramap[T](t => (t, strictSignal.now()))).apply(element)
        }
      }
    }
  }

}

object EventModifierBuilder {

  private def factory[El <: ReactivePixiElement.Base, T](
      observerToModifier: Observer[T] => PixiModifier[El]
  ): EventModifierBuilder[El, T] =
    (observer: Observer[T]) => observerToModifier(observer)

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
