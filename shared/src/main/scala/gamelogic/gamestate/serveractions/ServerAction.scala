package gamelogic.gamestate.serveractions

import gamelogic.gamestate.serveractions.ServerAction.ServerActionOutput
import gamelogic.gamestate.{ActionGatherer, GameAction}
import gamelogic.utils.IdGeneratorContainer
import gamelogic.gamestate.ActionGatherer

trait ServerAction {

  def apply(
      currentState: ActionGatherer,
      nowGenerator: () => Long
  )(using IdGeneratorContainer): (ActionGatherer, ServerAction.ServerActionOutput)

  def ++(that: ServerAction): ServerAction = {

    def app(
        currentState: ActionGatherer,
        nowGenerator: () => Long
    )(using IdGeneratorContainer): (ActionGatherer, ServerAction.ServerActionOutput) = {
      val (nextCollector, firstOutput) =
        this.apply(currentState, nowGenerator)
      val (lastCollector, secondOutput) =
        that.apply(nextCollector, nowGenerator)

      (lastCollector, firstOutput.merge(secondOutput))
    }

    new ServerAction {
      def apply(currentState: ActionGatherer, nowGenerator: () => Long)(using
          IdGeneratorContainer
      ): (ActionGatherer, ServerActionOutput) =
        app(currentState, nowGenerator)
    }
  }

}

object ServerAction {

  final case class ServerActionOutput(
      createdActions: Vector[GameAction],
      oldestTimeToRemove: Long,
      idsOfIdsToRemove: Vector[Long]
  ) {
    def merge(that: ServerActionOutput): ServerActionOutput = ServerActionOutput(
      createdActions ++ that.createdActions,
      oldestTimeToRemove min that.oldestTimeToRemove,
      idsOfIdsToRemove ++ that.idsOfIdsToRemove
    )
  }

}
