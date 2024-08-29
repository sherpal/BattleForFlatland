package utils.laminarzio

import zio.*
import com.raquo.laminar.api.L.*
import services.FrontendEnv
import scala.concurrent.ExecutionContext.Implicits.global
import com.raquo.laminar.nodes.ReactiveElement

private def runToFuture[E <: Throwable, A](effect: ZIO[FrontendEnv, E, A])(using
    runtime: Runtime[FrontendEnv]
) =
  Unsafe.unsafe(implicit unsafe =>
    runtime.unsafe.runToFuture(
      effect.catchAllCause(cause =>
        services.errorreporting.showCause(cause) *> ZIO.fail(cause.squashTrace)
      )
    )
  )

extension (es: EventStream.type) {
  def fromZIO[A](effect: ZIO[FrontendEnv, Nothing, A])(using Runtime[FrontendEnv]): EventStream[A] =
    EventStream.fromFuture(
      runToFuture(effect)
    )
}

extension [A](es: EventStream[A]) {
  def flatMapSwitchZIO[B](effect: A => ZIO[FrontendEnv, Nothing, B])(using
      Runtime[FrontendEnv]
  ): EventStream[B] =
    es.flatMapSwitch(a => EventStream.fromZIO(effect(a)))
}

extension (obs: Observer.type) {
  def fromZIO[Input](effect: Input => ZIO[FrontendEnv, Nothing, Unit])(using
      Runtime[FrontendEnv]
  ): Observer[Input] =
    obs.apply[Input](input => runToFuture(effect(input)).onComplete(_ => ()))
}

def onMountZIO(effect: ZIO[FrontendEnv, Nothing, Unit])(using
    runtime: Runtime[FrontendEnv]
): Modifier[HtmlElement] =
  onMountZIO(_ => effect)

def onMountZIO[El <: ReactiveElement.Base](
    effect: MountContext[El] => ZIO[FrontendEnv, Nothing, Unit]
)(using
    runtime: Runtime[FrontendEnv]
): Modifier[El] =
  onMountCallback(ctx => runToFuture(effect(ctx)).onComplete(_ => ()))

def onUnmountZIO[El <: ReactiveElement.Base](
    effect: El => ZIO[FrontendEnv, Nothing, Unit]
)(using Runtime[FrontendEnv]): Modifier[El] =
  onUnmountCallback(el => runToFuture(effect(el)).onComplete(_ => ()))

def onMountUnmountCallbackWithStateZIO[El <: ReactiveElement.Base, A](
    mount: MountContext[El] => ZIO[FrontendEnv, Nothing, A],
    unmount: (El, Option[A]) => ZIO[FrontendEnv, Nothing, Unit]
)(using Runtime[FrontendEnv]): Modifier[El] =
  onMountUnmountCallbackWithState[El, CancelableFuture[A]](
    ctx => runToFuture(mount(ctx)),
    { (el, maybeF) =>
      val effect =
        maybeF
          .map(f => ZIO.fromFuture(_ => f).asSome.flatMap(unmount(el, _)))
          .getOrElse(unmount(el, None))
      runToFuture(effect)
    }
  )
