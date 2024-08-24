package gamelogic.gamestate.gameactions

import gamelogic.abilities.SimpleBullet
import gamelogic.abilities.hexagon.FlashHeal
import gamelogic.gamestate.{GameAction, GameState, ImmutableActionCollector}
import gamelogic.physics.Complex
import models.bff.outofgame.PlayerClasses
import zio.{UIO, ZIO}
import zio.test.ZIOSpecDefault
import zio.test.Assertion._
import zio.test._

object ImmutableActionCollectorSpecs extends ZIOSpecDefault {
  def spec = suite("Action Collector behaviour")(
    test("Adding a GameStart action to an initial game state") {
      val collector = ImmutableActionCollector(GameState.empty)
      val gameStart = GameStart(0, 1)
      val endGame   = EndGame(1, 2)

      val (afterGameStart, _, _) = collector.masterAddAndRemoveActions(List(gameStart))

      for {
        gameIsStarted          <- assertTrue(afterGameStart.currentGameState.started)
        thereAreActions        <- assertTrue(afterGameStart.actionsAndStates.nonEmpty)
        gameStartActionIsThere <- assertTrue(afterGameStart.actionsAndStates.head._2 == List(gameStart))
        t                      <- ZIO.succeed(afterGameStart.masterAddAndRemoveActions(List(endGame)))
        (afterGameEnds, _, _) = t
        gameHasEnded <- assertTrue(afterGameEnds.currentGameState.ended)
      } yield gameIsStarted && thereAreActions && gameStartActionIsThere && gameHasEnded

    },
    test("Entity starts casting") {
      val collector = ImmutableActionCollector(GameState.empty)
      val gameStart = GameStart(0, 1)

      val newPlayer           = AddPlayerByClass(1L, 1L, 0L, Complex.zero, PlayerClasses.Hexagon, 0, "hexagon")
      val ability             = FlashHeal(0L, 2L, 0L, 0L)
      val entityStartsCasting = EntityStartsCasting(2L, 2, ability.castingTime, ability)

      val (collectorAfterActions, _, _) =
        collector.masterAddAndRemoveActions(List(gameStart, newPlayer, entityStartsCasting))

      assertTrue(collectorAfterActions.currentGameState.castingEntityInfo.keys.toList == List(0L))
    },
    test("Adding player out of order") {
      val collector    = ImmutableActionCollector(GameState.empty)
      val gameStart    = GameStart(0, 1)
      val firstPlayer  = AddPlayerByClass(0L, 3, 0L, 0, PlayerClasses.Square, 0, "square")
      val secondPlayer = AddPlayerByClass(1L, 2L, 1L, Complex.i, PlayerClasses.Hexagon, 2, "hexagon")

      for {
        afterGameStart    <- ZIO.succeed(collector.masterAddAndRemoveActions(gameStart :: Nil)).map(_._1)
        afterFirstPlayer  <- ZIO.succeed(afterGameStart.masterAddAndRemoveActions(firstPlayer :: Nil)).map(_._1)
        afterSecondPlayer <- ZIO.succeed(afterFirstPlayer.masterAddAndRemoveActions(secondPlayer :: Nil)).map(_._1)
        actionsInOrder    <- ZIO.succeed(afterSecondPlayer.actionsAndStates.head._2)
        result            <- assertTrue(actionsInOrder == List[GameAction](gameStart, secondPlayer, firstPlayer))
      } yield result
    },
    test("Adding two players makes gameState with two players") {
      val collector    = ImmutableActionCollector(GameState.empty)
      val gameStart    = GameStart(0, 1)
      val firstPlayer  = AddPlayerByClass(0L, 3, 0L, 0, PlayerClasses.Square, 0, "square")
      val secondPlayer = AddPlayerByClass(1L, 2L, 1L, Complex.i, PlayerClasses.Hexagon, 2, "hexagon")

      for {
        afterGameStart    <- ZIO.succeed(collector.masterAddAndRemoveActions(gameStart :: Nil)).map(_._1)
        afterFirstPlayer  <- ZIO.succeed(afterGameStart.masterAddAndRemoveActions(firstPlayer :: Nil)).map(_._1)
        firstPlayerCheck  <- assertTrue(afterFirstPlayer.currentGameState.players.size == 1)
        afterSecondPlayer <- ZIO.succeed(afterFirstPlayer.masterAddAndRemoveActions(secondPlayer :: Nil)).map(_._1)
        secondPlayerCheck <- assertTrue(afterSecondPlayer.currentGameState.players.size == 2)
      } yield firstPlayerCheck && secondPlayerCheck
    },
    test("Adding two players at the same time makes gameState with two players") {
      val collector = ImmutableActionCollector(GameState.empty)
      val actions = List(
        AddPlayerByClass(0L, 1589741103249L, 0L, 100.0, PlayerClasses.Hexagon, 3475617, "sherpal"),
        AddPlayerByClass(1L, 1589741103249L, 1L, -100.0, PlayerClasses.Square, 2780721, "machin"),
        GameStart(2, 1589741103265L)
      )

      for {
        withActions <- ZIO.succeed(collector.masterAddAndRemoveActions(actions)).map(_._1)
        result      <- assertTrue(withActions.currentGameState.players.keys.toList.sorted == List(0L, 1L))
      } yield result
    },
    test("Actions separated by more than the limit") {
      val collector = ImmutableActionCollector(GameState.empty, numberOfActionsBetweenGameStates = 2)
      val someDummyUpdates = (0 until 10).map(_.toLong).map { idx =>
        UpdateTimestamp(idx, idx * 4)
      }
      val gameStart    = GameStart(10L, 51L)
      val firstPlayer  = AddPlayerByClass(11L, 60L, 0L, 0, PlayerClasses.Square, 0, "square")
      val secondPlayer = AddPlayerByClass(12L, 52L, 1L, Complex.i, PlayerClasses.Square, 2, "square2")

      for {
        afterDummies <- ZIO.succeed(someDummyUpdates.foldLeft(collector) { (newCollector, action) =>
          newCollector.masterAddAndRemoveActions(action :: Nil)._1
        })
        afterGameStart    <- ZIO.succeed(afterDummies.masterAddAndRemoveActions(gameStart :: Nil)).map(_._1)
        afterSecondPlayer <- ZIO.succeed(afterGameStart.masterAddAndRemoveActions(secondPlayer :: Nil)).map(_._1)
        afterFirstPlayer  <- ZIO.succeed(afterSecondPlayer.masterAddAndRemoveActions(firstPlayer :: Nil)).map(_._1)
        actionsInOrder    <- ZIO.succeed(afterFirstPlayer.actionsAndStates.head._2)
        result <- assertTrue(actionsInOrder == List(firstPlayer)) // only first player should be there
      } yield result
    },
    test("Adding one player and starting game together") {
      val collector   = ImmutableActionCollector(GameState.empty)
      val gameStart   = GameStart(0, 2)
      val firstPlayer = AddPlayerByClass(0L, 1, 0L, 0, PlayerClasses.Square, 0, "square")

      val (after, _, _) = collector.masterAddAndRemoveActions(List(firstPlayer, gameStart))

      assertTrue(after.currentGameState.players.size == 1)
    }
  )
}
