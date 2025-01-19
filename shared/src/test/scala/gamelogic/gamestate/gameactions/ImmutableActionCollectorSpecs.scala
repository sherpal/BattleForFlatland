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
import gamelogic.utils.IdGeneratorContainer
import gamelogic.entities.Entity

object ImmutableActionCollectorSpecs extends ZIOSpecDefault {

  val idGen = IdGeneratorContainer.initialIdGeneratorContainer

  def spec = suite("Action Collector behaviour")(
    test("Adding a GameStart action to an initial game state") {
      val collector = ImmutableActionCollector(GameState.empty)
      val gameStart = GameStart(idGen.actionId(), 1)
      val endGame   = EndGame(idGen.actionId(), 2)

      val (afterGameStart, _, _) = collector.masterAddAndRemoveActions(Vector(gameStart))

      for {
        gameIsStarted   <- assertTrue(afterGameStart.currentGameState.started)
        thereAreActions <- assertTrue(afterGameStart.actionsAndStates.nonEmpty)
        gameStartActionIsThere <- assertTrue(
          afterGameStart.actionsAndStates.head._2 == Vector(gameStart)
        )
        t <- ZIO.succeed(afterGameStart.masterAddAndRemoveActions(Vector(endGame)))
        (afterGameEnds, _, _) = t
        gameHasEnded <- assertTrue(afterGameEnds.currentGameState.ended)
      } yield gameIsStarted && thereAreActions && gameStartActionIsThere && gameHasEnded

    },
    test("Entity starts casting") {
      val collector = ImmutableActionCollector(GameState.empty)
      val gameStart = GameStart(idGen.actionId(), 1)

      val playerId = idGen.entityId()

      val newPlayer =
        AddPlayerByClass(
          idGen.actionId(),
          1L,
          playerId,
          Complex.zero,
          PlayerClasses.Hexagon,
          0,
          "hexagon"
        )
      val ability = FlashHeal(idGen.abilityUseId(), 2L, playerId, playerId)
      val entityStartsCasting =
        EntityStartsCasting(idGen.actionId(), 2, ability.castingTime, ability)

      val (collectorAfterActions, _, _) =
        collector.masterAddAndRemoveActions(Vector(gameStart, newPlayer, entityStartsCasting))

      assertTrue(
        collectorAfterActions.currentGameState.castingEntityInfo.keys.toVector == List(playerId)
      )
    },
    test("Adding player out of order") {
      val collector      = ImmutableActionCollector(GameState.empty)
      val gameStart      = GameStart(idGen.actionId(), 1)
      val firstPlayerId  = idGen.entityId()
      val secondPlayerId = idGen.entityId()

      val firstPlayer =
        AddPlayerByClass(idGen.actionId(), 3, firstPlayerId, 0, PlayerClasses.Square, 0, "square")
      val secondPlayer =
        AddPlayerByClass(
          idGen.actionId(),
          2L,
          secondPlayerId,
          Complex.i,
          PlayerClasses.Hexagon,
          2,
          "hexagon"
        )

      for {
        afterGameStart <- ZIO
          .succeed(collector.masterAddAndRemoveActions(Vector(gameStart)))
          .map(_._1)
        afterFirstPlayer <- ZIO
          .succeed(afterGameStart.masterAddAndRemoveActions(Vector(firstPlayer)))
          .map(_._1)
        afterSecondPlayer <- ZIO
          .succeed(afterFirstPlayer.masterAddAndRemoveActions(Vector(secondPlayer)))
          .map(_._1)
        actionsInOrder <- ZIO.succeed(afterSecondPlayer.actionsAndStates.head._2)
        result <- assertTrue(
          actionsInOrder == List[GameAction](gameStart, secondPlayer, firstPlayer)
        )
      } yield result
    },
    test("Adding two players makes gameState with two players") {
      val collector     = ImmutableActionCollector(GameState.empty)
      val gameStart     = GameStart(idGen.actionId(), 1)
      val firstPlayerId = idGen.entityId()
      val firstPlayer = AddPlayerByClass(
        idGen.actionId(),
        3,
        idGen.entityId(),
        0,
        PlayerClasses.Square,
        0,
        "square"
      )
      val secondPlayerId = idGen.entityId()
      val secondPlayer =
        AddPlayerByClass(
          idGen.actionId(),
          2L,
          secondPlayerId,
          Complex.i,
          PlayerClasses.Hexagon,
          2,
          "hexagon"
        )

      for {
        afterGameStart <- ZIO
          .succeed(collector.masterAddAndRemoveActions(Vector(gameStart)))
          .map(_._1)
        afterFirstPlayer <- ZIO
          .succeed(afterGameStart.masterAddAndRemoveActions(Vector(firstPlayer)))
          .map(_._1)
        firstPlayerCheck <- assertTrue(afterFirstPlayer.currentGameState.players.size == 1)
        afterSecondPlayer <- ZIO
          .succeed(afterFirstPlayer.masterAddAndRemoveActions(Vector(secondPlayer)))
          .map(_._1)
        secondPlayerCheck <- assertTrue(afterSecondPlayer.currentGameState.players.size == 2)
      } yield firstPlayerCheck && secondPlayerCheck
    },
    test("Adding two players at the same time makes gameState with two players") {
      val collector      = ImmutableActionCollector(GameState.empty)
      val firstPlayerId  = idGen.entityId()
      val secondPlayerId = idGen.entityId()
      val actions = Vector(
        AddPlayerByClass(
          idGen.actionId(),
          1589741103249L,
          firstPlayerId,
          100.0,
          PlayerClasses.Hexagon,
          3475617,
          "sherpal"
        ),
        AddPlayerByClass(
          idGen.actionId(),
          1589741103249L,
          secondPlayerId,
          -100.0,
          PlayerClasses.Square,
          2780721,
          "machin"
        ),
        GameStart(idGen.actionId(), 1589741103265L)
      )

      for {
        withActions <- ZIO.succeed(collector.masterAddAndRemoveActions(actions)).map(_._1)
        result <- assertTrue(
          withActions.currentGameState.players.keys.toVector.sorted == Vector(
            firstPlayerId,
            secondPlayerId
          )
        )
      } yield result
    },
    test("Actions separated by more than the limit") {
      val collector =
        ImmutableActionCollector(GameState.empty, numberOfActionsBetweenGameStates = 2)
      val someDummyUpdates = (0 until 10).map(_.toLong).map { idx =>
        UpdateTimestamp(idGen.actionId(), idx * 4)
      }
      val gameStart     = GameStart(idGen.actionId(), 51L)
      val firstPlayerId = idGen.entityId()
      val firstPlayer =
        AddPlayerByClass(idGen.actionId(), 60L, firstPlayerId, 0, PlayerClasses.Square, 0, "square")
      val secondPlayerId = idGen.entityId()
      val secondPlayer =
        AddPlayerByClass(
          idGen.actionId(),
          52L,
          secondPlayerId,
          Complex.i,
          PlayerClasses.Square,
          2,
          "square2"
        )

      for {
        afterDummies <- ZIO.succeed(someDummyUpdates.foldLeft(collector) { (newCollector, action) =>
          newCollector.masterAddAndRemoveActions(Vector(action))._1
        })
        afterGameStart <- ZIO
          .succeed(afterDummies.masterAddAndRemoveActions(Vector(gameStart)))
          .map(_._1)
        afterSecondPlayer <- ZIO
          .succeed(afterGameStart.masterAddAndRemoveActions(Vector(secondPlayer)))
          .map(_._1)
        afterFirstPlayer <- ZIO
          .succeed(afterSecondPlayer.masterAddAndRemoveActions(Vector(firstPlayer)))
          .map(_._1)
        actionsInOrder <- ZIO.succeed(afterFirstPlayer.actionsAndStates.head._2)
        result <- assertTrue(
          actionsInOrder == List(firstPlayer)
        ) // only first player should be there
      } yield result
    },
    test("Adding one player and starting game together") {
      val collector     = ImmutableActionCollector(GameState.empty)
      val gameStart     = GameStart(idGen.actionId(), 2)
      val firstPlayerId = idGen.entityId()
      val firstPlayer =
        AddPlayerByClass(idGen.actionId(), 1, firstPlayerId, 0, PlayerClasses.Square, 0, "square")

      val (after, _, _) = collector.masterAddAndRemoveActions(Vector(firstPlayer, gameStart))

      assertTrue(after.currentGameState.players.size == 1)
    }
  )
}
