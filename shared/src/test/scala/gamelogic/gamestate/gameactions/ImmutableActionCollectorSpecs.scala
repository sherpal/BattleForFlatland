package gamelogic.gamestate.gameactions

import gamelogic.abilities.SimpleBullet
import gamelogic.gamestate.{GameAction, GameState, ImmutableActionCollector}
import gamelogic.physics.Complex
import zio.{UIO, ZIO}
import zio.test.DefaultRunnableSpec
import zio.test.Assertion._
import zio.test._

object ImmutableActionCollectorSpecs extends DefaultRunnableSpec {
  def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("Action Collector behaviour")(
    testM("Adding a GameStart action to an initial game state") {
      val collector = ImmutableActionCollector(GameState.initialGameState(0))
      val gameStart = GameStart(0, 1)
      val endGame   = EndGame(1, 2)

      val (afterGameStart, _, _) = collector.masterAddAndRemoveActions(List(gameStart))

      for {
        gameIsStarted <- assertM(UIO(afterGameStart.currentGameState.started))(equalTo(true))
        thereAreActions <- assertM(UIO(afterGameStart.actionsAndStates.nonEmpty))(equalTo(true))
        gameStartActionIsThere <- assertM(UIO(afterGameStart.actionsAndStates.head._2))(equalTo(List(gameStart)))
        t <- ZIO.effectTotal(afterGameStart.masterAddAndRemoveActions(List(endGame)))
        (afterGameEnds, _, _) = t
        gameHasEnded <- assertM(UIO(afterGameEnds.currentGameState.ended))(equalTo(true))
      } yield gameIsStarted && thereAreActions && gameStartActionIsThere && gameHasEnded

    },
    testM("Entity starts casting") {
      val collector = ImmutableActionCollector(GameState.initialGameState(0))
      val gameStart = GameStart(0, 1)

      val newPlayer           = AddPlayer(1, 1, 0L, Complex.zero, 0)
      val entityStartsCasting = EntityStartsCasting(2L, 2, SimpleBullet(0L, 2, 0L, Complex.zero, 0))

      val (collectorAfterActions, _, _) =
        collector.masterAddAndRemoveActions(List(gameStart, newPlayer, entityStartsCasting))

      assertM(UIO(collectorAfterActions.currentGameState.castingEntityInfo.keys.toList))(equalTo(List(0L)))
    },
    testM("Adding player out of order") {
      val collector    = ImmutableActionCollector(GameState.initialGameState(0L))
      val gameStart    = GameStart(0, 1)
      val firstPlayer  = AddPlayer(0, 3, 0L, 0, 0)
      val secondPlayer = AddPlayer(1L, 2L, 1L, Complex.i, 2)

      for {
        afterGameStart <- UIO(collector.masterAddAndRemoveActions(gameStart :: Nil)).map(_._1)
        afterFirstPlayer <- UIO(afterGameStart.masterAddAndRemoveActions(firstPlayer :: Nil)).map(_._1)
        afterSecondPlayer <- UIO(afterFirstPlayer.masterAddAndRemoveActions(secondPlayer :: Nil)).map(_._1)
        actionsInOrder <- UIO(afterSecondPlayer.actionsAndStates.head._2)
        result <- assertM(UIO(actionsInOrder))(equalTo(List[GameAction](gameStart, secondPlayer, firstPlayer)))
      } yield result
    },
    testM("Actions separated by more than the limit") {
      val collector    = ImmutableActionCollector(GameState.initialGameState(0L), timeBetweenGameStates = 3L)
      val gameStart    = GameStart(0, 1)
      val firstPlayer  = AddPlayer(0, 4, 0L, 0, 0)
      val secondPlayer = AddPlayer(1L, 2L, 1L, Complex.i, 2)

      for {
        afterGameStart <- UIO(collector.masterAddAndRemoveActions(gameStart :: Nil)).map(_._1)
        afterSecondPlayer <- UIO(afterGameStart.masterAddAndRemoveActions(secondPlayer :: Nil)).map(_._1)
        afterFirstPlayer <- UIO(afterSecondPlayer.masterAddAndRemoveActions(firstPlayer :: Nil)).map(_._1)
        actionsInOrder <- UIO(afterFirstPlayer.actionsAndStates.head._2)
        result <- assertM(UIO(actionsInOrder))(equalTo(List(firstPlayer))) // only first player should be there
      } yield result
    },
    testM("Adding one player and starting game together") {
      val collector   = ImmutableActionCollector(GameState.initialGameState(0L))
      val gameStart   = GameStart(0, 2)
      val firstPlayer = AddPlayer(0, 1, 0L, 0, 0)

      val (after, _, toRemove) = collector.masterAddAndRemoveActions(List(firstPlayer, gameStart))

      assertM(UIO(after.currentGameState.players.size))(equalTo(1))
    }
  )
}
