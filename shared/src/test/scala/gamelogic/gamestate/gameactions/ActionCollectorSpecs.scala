package gamelogic.gamestate.gameactions

import gamelogic.abilities.SimpleBullet
import gamelogic.gamestate.{ActionCollector, GameAction, GameState}
import gamelogic.physics.Complex
import zio.{UIO, ZIO}
import zio.test.DefaultRunnableSpec
import zio.test.Assertion._
import zio.test._

object ActionCollectorSpecs extends DefaultRunnableSpec {
  def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("Action Collector behaviour")(
    testM("Adding a GameStart action to an initial game state") {
      val collector = new ActionCollector(GameState.initialGameState(0))
      val gameStart = GameStart(0, 1)
      val endGame   = EndGame(1, 2)

      collector.addAndRemoveActions(List(gameStart))

      for {
        gameIsStarted <- assertM(UIO(collector.currentGameState.started))(equalTo(true))
        thereAreActions <- assertM(UIO(collector.testActionAndStates.nonEmpty))(equalTo(true))
        gameStartActionIsThere <- assertM(UIO(collector.testActionAndStates.head._2))(equalTo(List(gameStart)))
        _ <- ZIO.effectTotal(collector.addAndRemoveActions(List(endGame)))
        gameHasEnded <- assertM(UIO(collector.currentGameState.ended))(equalTo(true))
      } yield gameIsStarted && thereAreActions && gameStartActionIsThere && gameHasEnded

    },
    testM("Entity starts casting") {
      val collector = new ActionCollector(GameState.initialGameState(0))
      val gameStart = GameStart(0, 1)

      val newPlayer           = AddPlayer(1, 1, 0L, Complex.zero, 0)
      val entityStartsCasting = EntityStartsCasting(2L, 2, SimpleBullet(0L, 2, 0L, Complex.zero, 0))

      collector.addAndRemoveActions(List(gameStart, newPlayer, entityStartsCasting))

      assertM(UIO(collector.currentGameState.castingEntityInfo.keys.toList))(equalTo(List(0L)))
    },
    testM("Adding player out of order") {
      val collector    = new ActionCollector(GameState.initialGameState(0L))
      val gameStart    = GameStart(0, 1)
      val firstPlayer  = AddPlayer(0, 3, 0L, 0, 0)
      val secondPlayer = AddPlayer(1L, 2L, 1L, Complex.i, 2)

      for {
        _ <- ZIO.effect(collector.addAndRemoveActions(gameStart :: Nil))
        _ <- ZIO.effect(collector.addAndRemoveActions(firstPlayer :: Nil))
        _ <- ZIO.effect(collector.addAndRemoveActions(secondPlayer :: Nil))
        actionsInOrder <- UIO(collector.testActionAndStates.head._2)
        result <- assertM(UIO(actionsInOrder))(equalTo(List[GameAction](gameStart, secondPlayer, firstPlayer)))
      } yield result
    },
    testM("Actions separated by more than the limit") {
      val collector    = new ActionCollector(GameState.initialGameState(0L), timeBetweenGameStates = 3L)
      val gameStart    = GameStart(0, 1)
      val firstPlayer  = AddPlayer(0, 4, 0L, 0, 0)
      val secondPlayer = AddPlayer(1L, 2L, 1L, Complex.i, 2)

      for {
        _ <- ZIO.effect(collector.addAndRemoveActions(gameStart :: Nil))
        _ <- ZIO.effect(collector.addAndRemoveActions(secondPlayer :: Nil))
        _ <- ZIO.effect(collector.addAndRemoveActions(firstPlayer :: Nil))
        actionsInOrder <- UIO(collector.testActionAndStates.head._2)
        result <- assertM(UIO(actionsInOrder))(equalTo(List(firstPlayer))) // only first player should be there
      } yield result
    }
  )
}
