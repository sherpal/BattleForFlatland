package gamelogic.gamestate

import gamelogic.gamestate.gameactions.UpdateTimestamp
import gamelogic.utils.IdGeneratorContainer
import zio.ZIO
import zio.test.*

object AddAndRemoveActionsSpecs extends ZIOSpecDefault {

  val idGen = IdGeneratorContainer.initialIdGeneratorContainer

  def spec = suite("AddAndRemoveActions")(
    test("Refused actions are not sent, and receivers end up in the same state") {
      val kept         = UpdateTimestamp(idGen.actionId(), 1)
      val refused      = UpdateTimestamp(idGen.actionId(), 3)
      val alsoKept     = UpdateTimestamp(idGen.actionId(), 2)
      val olderRemoved = idGen.actionId()
      val update =
        AddAndRemoveActions(Vector(kept, refused, alsoKept), 0L, Vector(refused.id, olderRemoved))

      val sent = update.withoutRefusedActions
      def received(update: AddAndRemoveActions) = GreedyActionGatherer(GameState.empty)
        .slaveAddAndRemoveActions(
          update.actionsToAdd,
          update.oldestTimeToRemove,
          update.idsOfActionsToRemove
        )
        .currentGameState

      ZIO.succeed(
        assertTrue(
          sent.actionsToAdd == Vector(kept, alsoKept),
          sent.idsOfActionsToRemove == Vector(olderRemoved),
          received(sent) == received(update),
          received(sent).time == 2L
        )
      )
    }
  )
}
