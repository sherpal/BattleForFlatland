package gamelogic.gamestate.gameactions

import gamelogic.abilities.SimpleBullet
import gamelogic.abilities.hexagon.FlashHeal
import gamelogic.gamestate.{GameAction, GameState, ImmutableActionCollector}
import gamelogic.physics.Complex
import models.bff.outofgame.PlayerClasses
import zio.{UIO, ZIO}
import zio.test.DefaultRunnableSpec
import zio.test.Assertion._
import zio.test._

object ImmutableActionCollectorSpecs extends DefaultRunnableSpec {
  def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("Action Collector behaviour")(
    testM("Adding a GameStart action to an initial game state") {
      val collector = ImmutableActionCollector(GameState.empty)
      val gameStart = GameStart(0, 1)
      val endGame   = EndGame(1, 2)

      val (afterGameStart, _, _) = collector.masterAddAndRemoveActions(List(gameStart))

      for {
        gameIsStarted          <- assertM(UIO(afterGameStart.currentGameState.started))(equalTo(true))
        thereAreActions        <- assertM(UIO(afterGameStart.actionsAndStates.nonEmpty))(equalTo(true))
        gameStartActionIsThere <- assertM(UIO(afterGameStart.actionsAndStates.head._2))(equalTo(List(gameStart)))
        t                      <- ZIO.effectTotal(afterGameStart.masterAddAndRemoveActions(List(endGame)))
        (afterGameEnds, _, _) = t
        gameHasEnded <- assertM(UIO(afterGameEnds.currentGameState.ended))(equalTo(true))
      } yield gameIsStarted && thereAreActions && gameStartActionIsThere && gameHasEnded

    },
    testM("Entity starts casting") {
      val collector = ImmutableActionCollector(GameState.empty)
      val gameStart = GameStart(0, 1)

      val newPlayer           = AddPlayerByClass(1L, 1L, 0L, Complex.zero, PlayerClasses.Hexagon, 0, "hexagon")
      val ability             = FlashHeal(0L, 2L, 0L, 0L)
      val entityStartsCasting = EntityStartsCasting(2L, 2, ability.castingTime, ability)

      val (collectorAfterActions, _, _) =
        collector.masterAddAndRemoveActions(List(gameStart, newPlayer, entityStartsCasting))

      assertM(UIO(collectorAfterActions.currentGameState.castingEntityInfo.keys.toList))(equalTo(List(0L)))
    },
    testM("Adding player out of order") {
      val collector    = ImmutableActionCollector(GameState.empty)
      val gameStart    = GameStart(0, 1)
      val firstPlayer  = AddPlayerByClass(0L, 3, 0L, 0, PlayerClasses.Square, 0, "square")
      val secondPlayer = AddPlayerByClass(1L, 2L, 1L, Complex.i, PlayerClasses.Hexagon, 2, "hexagon")

      for {
        afterGameStart    <- UIO(collector.masterAddAndRemoveActions(gameStart :: Nil)).map(_._1)
        afterFirstPlayer  <- UIO(afterGameStart.masterAddAndRemoveActions(firstPlayer :: Nil)).map(_._1)
        afterSecondPlayer <- UIO(afterFirstPlayer.masterAddAndRemoveActions(secondPlayer :: Nil)).map(_._1)
        actionsInOrder    <- UIO(afterSecondPlayer.actionsAndStates.head._2)
        result            <- assertM(UIO(actionsInOrder))(equalTo(List[GameAction](gameStart, secondPlayer, firstPlayer)))
      } yield result
    },
    testM("Adding two players makes gameState with two players") {
      val collector    = ImmutableActionCollector(GameState.empty)
      val gameStart    = GameStart(0, 1)
      val firstPlayer  = AddPlayerByClass(0L, 3, 0L, 0, PlayerClasses.Square, 0, "square")
      val secondPlayer = AddPlayerByClass(1L, 2L, 1L, Complex.i, PlayerClasses.Hexagon, 2, "hexagon")

      for {
        afterGameStart    <- UIO(collector.masterAddAndRemoveActions(gameStart :: Nil)).map(_._1)
        afterFirstPlayer  <- UIO(afterGameStart.masterAddAndRemoveActions(firstPlayer :: Nil)).map(_._1)
        firstPlayerCheck  <- assertM(UIO(afterFirstPlayer.currentGameState.players.size))(equalTo(1))
        afterSecondPlayer <- UIO(afterFirstPlayer.masterAddAndRemoveActions(secondPlayer :: Nil)).map(_._1)
        secondPlayerCheck <- assertM(UIO(afterSecondPlayer.currentGameState.players.size))(equalTo(2))
      } yield firstPlayerCheck && secondPlayerCheck
    },
    testM("Adding two players at the same time makes gameState with two players") {
      val collector = ImmutableActionCollector(GameState.empty)
      val actions = List(
        AddPlayerByClass(0L, 1589741103249L, 0L, 100.0, PlayerClasses.Hexagon, 3475617, "sherpal"),
        AddPlayerByClass(1L, 1589741103249L, 1L, -100.0, PlayerClasses.Square, 2780721, "machin"),
        GameStart(2, 1589741103265L)
      )

      for {
        withActions <- UIO { collector.masterAddAndRemoveActions(actions) }.map(_._1)
        result      <- assertM(UIO(withActions.currentGameState.players.keys.toList.sorted))(equalTo(List(0L, 1L)))
      } yield result
    },
    testM("Actions separated by more than the limit") {
      val collector = ImmutableActionCollector(GameState.empty, numberOfActionsBetweenGameStates = 2)
      val someDummyUpdates = (0 until 10).map(_.toLong).map { idx =>
        UpdateTimestamp(idx, idx * 4)
      }
      val gameStart    = GameStart(10L, 51L)
      val firstPlayer  = AddPlayerByClass(11L, 60L, 0L, 0, PlayerClasses.Square, 0, "square")
      val secondPlayer = AddPlayerByClass(12L, 52L, 1L, Complex.i, PlayerClasses.Square, 2, "square2")

      for {
        afterDummies <- UIO(someDummyUpdates.foldLeft(collector) { (newCollector, action) =>
          newCollector.masterAddAndRemoveActions(action :: Nil)._1
        })
        afterGameStart    <- UIO(afterDummies.masterAddAndRemoveActions(gameStart :: Nil)).map(_._1)
        afterSecondPlayer <- UIO(afterGameStart.masterAddAndRemoveActions(secondPlayer :: Nil)).map(_._1)
        afterFirstPlayer  <- UIO(afterSecondPlayer.masterAddAndRemoveActions(firstPlayer :: Nil)).map(_._1)
        actionsInOrder    <- UIO(afterFirstPlayer.actionsAndStates.head._2)
        result            <- assertM(UIO(actionsInOrder))(equalTo(List(firstPlayer))) // only first player should be there
      } yield result
    },
    testM("Adding one player and starting game together") {
      val collector   = ImmutableActionCollector(GameState.empty)
      val gameStart   = GameStart(0, 2)
      val firstPlayer = AddPlayerByClass(0L, 1, 0L, 0, PlayerClasses.Square, 0, "square")

      val (after, _, _) = collector.masterAddAndRemoveActions(List(firstPlayer, gameStart))

      assertM(UIO(after.currentGameState.players.size))(equalTo(1))
    }
  )
}
