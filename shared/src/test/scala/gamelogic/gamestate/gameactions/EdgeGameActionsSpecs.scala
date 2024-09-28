package gamelogic.gamestate.gameactions

import gamelogic.gamestate.GameState
import zio.ZIO
import zio.test.ZIOSpecDefault
import zio.test.Assertion.*
import zio.test.*
import gamelogic.utils.IdGeneratorContainer

object EdgeGameActionsSpecs extends ZIOSpecDefault {

  val idGen = IdGeneratorContainer.initialIdGeneratorContainer

  def spec = suite("Edge game actions")(
    test("Game must be started after GameStart action") {
      check(Gen.long(0, Long.MaxValue), Gen.long(0, Long.MaxValue)) { (time, startingTime) =>
        val gameStart = GameStart(idGen.actionId(), startingTime)
        val gameState = GameState.empty.copy(time = time)
        ZIO.succeed(assertTrue(gameStart(gameState).started))
      }
    },
    test("Game must be ended after EndGame action") {
      val gameEnd   = EndGame(idGen.actionId(), 1)
      val gameState = GameState.empty

      ZIO.succeed(assertTrue(gameEnd(gameState).ended))
    },
    test("Applying a time update on a game state changes its time") {
      check(Gen.long, Gen.long) { (time, updateTime) =>
        val updateTimestamp = UpdateTimestamp(idGen.actionId(), updateTime)
        val gameState       = GameState.empty.copy(time = time)
        ZIO.succeed(assertTrue(updateTimestamp(gameState).time == updateTime.max(time).max(0)))
      }
    }
  )
}
