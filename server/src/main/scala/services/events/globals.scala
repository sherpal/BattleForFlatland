package services.events

import zio.*
import scala.reflect.Typeable

def registerEvents(subscription: Subscription): ZIO[Events, Nothing, Unit] =
  ZIO.serviceWithZIO[Events](_.registerEvents(subscription))

def registerEvents[InterestedIn <: Event](whileAlive: ZIO[Any, Nothing, Boolean])(using
    Typeable[InterestedIn]
)(
    handler: InterestedIn => ZIO[Any, Nothing, Unit]
): ZIO[Events, Nothing, Unit] =
  val finalHandler: Event => ZIO[Any, Nothing, Unit] = {
    case event: InterestedIn => handler(event)
    case _                   => ZIO.unit
  }
  registerEvents(Subscription(finalHandler, whileAlive))

def dispatchEvent(event: Event): ZIO[Events, Nothing, Unit] =
  ZIO.serviceWithZIO[Events](_.dispatchEvent(event))
