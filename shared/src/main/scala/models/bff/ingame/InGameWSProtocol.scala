package models.bff.ingame

import gamelogic.gamestate.GameAction
import io.circe.generic.extras.Configuration
import io.circe.{Decoder, Encoder}

sealed trait InGameWSProtocol

object InGameWSProtocol {

  /** Messages coming from the server to the client */
  sealed trait Incoming extends InGameWSProtocol

  /** Messages going to the server from the client */
  sealed trait Outgoing extends InGameWSProtocol

  case object Hello extends Incoming
  case object HeartBeat extends Incoming

  case class Ping(sendingTime: Long) extends Outgoing
  case class Pong(originalSendingTime: Long, midwayDistantTime: Long) extends Incoming

  case class GameActionWrapper(gameActions: List[GameAction]) extends Outgoing
  case class RemoveActions(oldestTime: Long, idsOfActionsToRemove: List[GameAction.Id]) extends Incoming
  case class AddAndRemoveActions(
      actionsToAdd: List[GameAction],
      oldestTimeToRemove: Long,
      idsOfActionsToRemove: List[GameAction.Id]
  ) extends Incoming

  import io.circe.generic.extras.semiauto._
  implicit val genDevConfig: Configuration =
    Configuration.default.withDiscriminator("what_am_i_in_game")

  implicit def decoder: Decoder[InGameWSProtocol] = deriveConfiguredDecoder
  implicit def encoder: Encoder[InGameWSProtocol] = deriveConfiguredEncoder
}
