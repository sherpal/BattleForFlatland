package gamelogic.gamestate.statetransformers

import gamelogic.gamestate.gameactions.{EndGame, GameStart}
import gamelogic.gamestate.{GameState, ImmutableActionCollector}
import zio.UIO
import zio.test.ZIOSpecDefault
import zio.test.Assertion.*
import zio.test.*
import gamelogic.utils.IdGeneratorContainer

object GameStateTransformerSpecs extends ZIOSpecDefault {

  val idGen = IdGeneratorContainer.initialIdGeneratorContainer

  def spec = suite("Game state transformers")(
    test("Starting and ending game at once should give same result") {
      val gameState = GameState.empty
      val gameStart = GameStart(idGen.actionId(), 1)
      val endGame   = EndGame(idGen.actionId(), 2)

      assertTrue(
        gameState.applyActions(Vector(gameStart, endGame)).endTime ==
          Vector(gameStart, endGame)
            .map(_.createGameStateTransformer(gameState))
            .foldLeft(
              GameStateTransformer.identityTransformer
            )(_ ++ _)(gameState)
            .endTime
      )

    }
  )

}
