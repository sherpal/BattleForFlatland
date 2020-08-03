package gamelogic.gamestate.statetransformers

import gamelogic.gamestate.gameactions.{EndGame, GameStart}
import gamelogic.gamestate.{GameState, ImmutableActionCollector}
import zio.UIO
import zio.test.DefaultRunnableSpec
import zio.test.Assertion._
import zio.test._

object GameStateTransformerSpecs extends DefaultRunnableSpec {

  def spec = suite("Game state transformers")(
    testM("Starting and ending game at once should give same result") {
      val gameState = GameState.empty
      val gameStart = GameStart(0, 1)
      val endGame   = EndGame(1, 2)

      assertM(UIO(gameState.applyActions(List(gameStart, endGame)).endTime))(
        equalTo(
          List(gameStart, endGame)
            .map(_.createGameStateTransformer(gameState))
            .foldLeft(
              GameStateTransformer.identityTransformer
            )(_ ++ _)(gameState)
            .endTime
        )
      )

    }
  )

}
