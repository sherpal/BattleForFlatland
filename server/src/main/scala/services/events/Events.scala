package services.events

import zio.*

trait Events {

  def registerEvents(subscription: Subscription): ZIO[Any, Nothing, Unit]

  def dispatchEvent(event: Event): ZIO[Any, Nothing, Unit]

}

object Events {

  val live = ZLayer.fromZIO(for {
    _       <- Console.printLine("Initializing Events service...")
    ref     <- Ref.make[Vector[Subscription]](Vector.empty)
    queue   <- Queue.bounded[Event](10000)
    runtime <- ZIO.runtime[Any]
    bEvents <- ZIO.succeed(BEvents(ref, queue, runtime))
  } yield (bEvents: Events))

}
