package services.events

import zio.*

private[events] class BEvents(
    subscriptionsRef: Ref[Vector[Subscription]],
    eventQueue: Queue[Event],
    runtime: Runtime[Any]
) extends Events {

  override def registerEvents(subscription: Subscription): ZIO[Any, Nothing, Unit] =
    subscriptionsRef.update(_ :+ subscription)

  override def dispatchEvent(event: Event): ZIO[Any, Nothing, Unit] = eventQueue.offer(event).unit

  val eventDispatcher = zio.stream.ZStream
    .fromQueue(eventQueue, 1)
    .tap(event =>
      for {
        _                    <- Console.printLine(s"Dispatching $event...").orDie
        currentSubscriptions <- subscriptionsRef.get
        numberOfCurrentSubscriptions = currentSubscriptions.length
        aliveSubscriptions <- ZIO.filter(currentSubscriptions)(
          _.alive.catchAllCause(_ => ZIO.succeed(false))
        )
        _ <- ZIO.foreachDiscard(aliveSubscriptions)(
          _.handler(event).catchAllCause { cause =>
            println("howza! handler failed")
            ZIO.succeed(cause.squash.printStackTrace())
          }
        )
        _ <- subscriptionsRef.update(subscriptionsNow =>
          // we can't simply set, in case a subscription was registered in the meantime
          aliveSubscriptions ++ subscriptionsNow.drop(numberOfCurrentSubscriptions)
        )
      } yield ()
    )
    .runDrain

  Unsafe.unsafe(implicit unsafe => runtime.unsafe.runOrFork(eventDispatcher)) match {
    case Left(value)                => println(s"Fiber for event dispatcher: $value")
    case Right(Exit.Success(_))     => throw IllegalStateException(s"Event Dispatcher finished.")
    case Right(Exit.Failure(cause)) => throw cause.squash
  }

  println("Events service initialized.")

}
