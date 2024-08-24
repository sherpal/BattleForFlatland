package gamelogic.gamestate.statetransformers

import gamelogic.gamestate.gameactions.{EndGame, GameStart}
import gamelogic.gamestate.{GameState, ImmutableActionCollector}
import zio.UIO
import zio.test.ZIOSpecDefault
import zio.test.Assertion.*
import zio.test.*

object GameStateTransformerSpecs extends ZIOSpecDefault {

  def spec = suite("Game state transformers")(
    test("Starting and ending game at once should give same result") {
      val gameState = GameState.empty
      val gameStart = GameStart(0, 1)
      val endGame   = EndGame(1, 2)

      assertTrue(
        gameState.applyActions(List(gameStart, endGame)).endTime ==
          List(gameStart, endGame)
            .map(_.createGameStateTransformer(gameState))
            .foldLeft(
              GameStateTransformer.identityTransformer
            )(_ ++ _)(gameState)
            .endTime
      )

    }
  )

}
