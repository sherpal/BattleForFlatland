package gamelogic.gamestate.gameactions

import gamelogic.gamestate.GameState
import zio.UIO
import zio.test.DefaultRunnableSpec
import zio.test.Assertion._
import zio.test._

object EdgeGameActionsSpecs extends DefaultRunnableSpec {

  def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("Edge game actions")(
    testM("Game must be started after GameStart action") {
      checkM(Gen.long(0, Long.MaxValue), Gen.long(0, Long.MaxValue)) { (time, startingTime) =>
        val gameStart = GameStart(0, startingTime)
        val gameState = GameState.initialGameState(time)
        assertM(UIO(gameStart(gameState).started))(equalTo(true))
      }
    },
    testM("Game must be ended after EndGame action") {
      val gameEnd   = EndGame(0, 1)
      val gameState = GameState.initialGameState(0)

      assertM(UIO(gameEnd(gameState).ended))(equalTo(true))
    },
    testM("Applying a time update on a game state changes its time") {
      checkM(Gen.anyLong, Gen.anyLong) { (time, updateTime) =>
        val updateTimestamp = UpdateTimestamp(0, updateTime)
        val gameState       = GameState.initialGameState(time)
        assertM(UIO(updateTimestamp(gameState).time))(equalTo(updateTime))
      }
    }
  )
}
