package services.events

trait Event

object Event {
  case class Ping() extends Event

  // Some(gameId) for a specific game, None for all
  case class GameDataRefreshed(maybeGameId: Option[String]) extends Event
}
