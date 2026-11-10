package gamelogic.gamestate.gameactions

import gamelogic.gamestate.GameState
import gamelogic.utils.IdGeneratorContainer
import org.scalacheck._
import org.scalacheck.Prop._

object EdgeGameActionsSpecs extends Properties("Edge game actions") {

  val idGen = IdGeneratorContainer.initialIdGeneratorContainer

  property("Game must be started after GameStart action") =
    forAll(Gen.choose(0L, Long.MaxValue), Gen.choose(0L, Long.MaxValue)) { (time, startingTime) =>
      val gameStart = GameStart(idGen.actionId(), startingTime)
      val gameState = GameState.empty.copy(time = time)
      gameStart(gameState).started
    }

  property("Game must be ended after EndGame action") = {
    val gameEnd   = EndGame(idGen.actionId(), 1)
    val gameState = GameState.empty

    Prop(gameEnd(gameState).ended)
  }

  property("Applying a time update on a game state changes its time") =
    forAll(Arbitrary.arbitrary[Long], Arbitrary.arbitrary[Long]) { (time, updateTime) =>
      val updateTimestamp = UpdateTimestamp(idGen.actionId(), updateTime)
      val gameState       = GameState.empty.copy(time = time)
      updateTimestamp(gameState).time == updateTime
    }
}
