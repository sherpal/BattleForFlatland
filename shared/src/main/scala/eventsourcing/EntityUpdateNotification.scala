package eventsourcing

import io.circe.{Decoder, Encoder}

case class EntityUpdateNotification[State](state: State, sequenceNumber: Int)

object EntityUpdateNotification {
  given [State](using Encoder[State]): Encoder[EntityUpdateNotification[State]] =
    io.circe.generic.semiauto.deriveEncoder
  given [State](using Decoder[State]): Decoder[EntityUpdateNotification[State]] =
    io.circe.generic.semiauto.deriveDecoder
}
