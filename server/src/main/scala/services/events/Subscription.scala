package services.events

final case class Subscription(
    handler: Event => Unit,
    alive: () => Boolean
)
