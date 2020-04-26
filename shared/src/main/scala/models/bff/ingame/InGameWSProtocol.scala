package models.bff.ingame

import gamelogic.gamestate.GameAction
import io.circe.generic.extras.Configuration
import io.circe.{Decoder, Encoder}

sealed trait InGameWSProtocol

object InGameWSProtocol {

  case object Hello extends InGameWSProtocol
  case object HeartBeat extends InGameWSProtocol
  case class GameActionWrapper(gameActions: List[GameAction]) extends InGameWSProtocol
  case class RemoveActions(oldestTime: Long, idsOfActionsToRemove: List[GameAction.Id]) extends InGameWSProtocol
  case class AddAndRemoveActions(
      actionsToAdd: List[GameAction],
      oldestTimeToRemove: Long,
      idsOfActionsToRemove: List[GameAction.Id]
  ) extends InGameWSProtocol

  import io.circe.generic.extras.semiauto._
  implicit val genDevConfig: Configuration =
    Configuration.default.withDiscriminator("what_am_i_in_game")

  implicit def decoder: Decoder[InGameWSProtocol] = deriveConfiguredDecoder
  implicit def encoder: Encoder[InGameWSProtocol] = deriveConfiguredEncoder
}
