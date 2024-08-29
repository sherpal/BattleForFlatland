package services.events

import zio.*

def registerEvents(subscription: Subscription): ZIO[Events, Nothing, Unit] =
  ZIO.serviceWithZIO[Events](_.registerEvents(subscription))

def registerEvents(whileAlive: ZIO[Any, Nothing, Boolean])(
    handler: PartialFunction[Event, ZIO[Any, Nothing, Unit]]
): ZIO[Events, Nothing, Unit] =
  val finalHandler = (event: Event) =>
    if handler.isDefinedAt(event) then handler(event) else ZIO.unit
  registerEvents(Subscription(finalHandler, whileAlive))

def dispatchEvent(event: Event): ZIO[Events, Nothing, Unit] =
  ZIO.serviceWithZIO[Events](_.dispatchEvent(event))
