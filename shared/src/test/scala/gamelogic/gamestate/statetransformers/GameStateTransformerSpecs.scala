package gamelogic.gamestate.statetransformers

import gamelogic.gamestate.gameactions.{EndGame, GameStart}
import gamelogic.gamestate.GameState
import gamelogic.utils.IdGeneratorContainer

final class GameStateTransformerSpecs extends munit.FunSuite {

  val idGen = IdGeneratorContainer.initialIdGeneratorContainer

  test("Starting and ending game at once should give same result") {
    val gameState = GameState.empty
    val gameStart = GameStart(idGen.actionId(), 1)
    val endGame   = EndGame(idGen.actionId(), 2)

    assertEquals(
      gameState.applyActions(Vector(gameStart, endGame)).endTime,
      Vector(gameStart, endGame)
        .map(_.createGameStateTransformer(gameState))
        .foldLeft(
          GameStateTransformer.identityTransformer
        )(_ ++ _)(gameState)
        .endTime
    )

  }

}
