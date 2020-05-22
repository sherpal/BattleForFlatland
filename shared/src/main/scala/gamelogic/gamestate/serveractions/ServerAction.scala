package gamelogic.gamestate.serveractions

import gamelogic.gamestate.serveractions.ServerAction.ServerActionOutput
import gamelogic.gamestate.{GameAction, ImmutableActionCollector}
import gamelogic.utils.IdGeneratorContainer

trait ServerAction {

  def apply(
      currentState: ImmutableActionCollector,
      nowGenerator: () => Long
  )(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): (ImmutableActionCollector, ServerAction.ServerActionOutput)

  /** Isn't this Kleisli? */
  def ++(that: ServerAction): ServerAction = {

    def app(
        currentState: ImmutableActionCollector,
        nowGenerator: () => Long
    )(
        implicit idGeneratorContainer: IdGeneratorContainer
    ): (ImmutableActionCollector, ServerAction.ServerActionOutput) = {
      val (nextCollector, firstOutput) =
        this.apply(currentState, nowGenerator)
      val (lastCollector, secondOutput) =
        that.apply(nextCollector, nowGenerator)

      (lastCollector, firstOutput merge secondOutput)
    }

    new ServerAction {
      def apply(currentState: ImmutableActionCollector, nowGenerator: () => Long)(
          implicit idGeneratorContainer: IdGeneratorContainer
      ): (ImmutableActionCollector, ServerActionOutput) =
        app(currentState, nowGenerator)
    }
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
