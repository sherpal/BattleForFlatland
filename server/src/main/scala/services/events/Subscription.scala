package services.events

import zio.ZIO

final case class Subscription(
    handler: Event => ZIO[Any, Nothing, Unit],
    alive: ZIO[Any, Nothing, Boolean]
)
