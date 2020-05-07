package gamelogic.gamestate.serveractions

import gamelogic.gamestate.serveractions.ServerAction.ServerActionOutput
import gamelogic.gamestate.{GameAction, ImmutableActionCollector}
import gamelogic.utils.{AbilityUseIdGenerator, EntityIdGenerator, GameActionIdGenerator}

trait ServerAction {

  def apply(
      currentState: ImmutableActionCollector,
      gameActionIdGenerator: GameActionIdGenerator,
      entityIdGenerator: EntityIdGenerator,
      abilityUseIdGenerator: AbilityUseIdGenerator,
      nowGenerator: () => Long
  ): (ImmutableActionCollector, ServerActionOutput)

  /** Isn't this Kleisli? */
  def ++(that: ServerAction): ServerAction =
    (
        currentState: ImmutableActionCollector,
        gameActionIdGenerator: GameActionIdGenerator,
        entityIdGenerator: EntityIdGenerator,
        abilityUseIdGenerator: AbilityUseIdGenerator,
        nowGenerator: () => Long
    ) => {
      val (nextCollector, firstOuput) =
        this.apply(currentState, gameActionIdGenerator, entityIdGenerator, abilityUseIdGenerator, nowGenerator)
      val (lastCollector, secondOuput) =
        that.apply(nextCollector, gameActionIdGenerator, entityIdGenerator, abilityUseIdGenerator, nowGenerator)

      (lastCollector, firstOuput merge secondOuput)
    }

}

object ServerAction {

  final case class ServerActionOutput(
      createdActions: List[GameAction],
      oldestTimeToRemove: Long,
      idsOfIdsToRemove: List[Long]
  ) {
    def merge(that: ServerActionOutput): ServerActionOutput = ServerActionOutput(
      createdActions ++ that.createdActions,
      oldestTimeToRemove min that.oldestTimeToRemove,
      idsOfIdsToRemove ++ that.idsOfIdsToRemove
    )
  }

}
