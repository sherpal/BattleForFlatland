package gamelogic.gamestate.gameactions

import gamelogic.abilities.hexagon.FlashHeal
import gamelogic.gamestate.{GameAction, GameState, ImmutableActionCollector}
import gamelogic.physics.Complex
import models.bff.outofgame.PlayerClasses
import gamelogic.utils.IdGeneratorContainer

final class ImmutableActionCollectorSpecs extends munit.FunSuite {

  val idGen = IdGeneratorContainer.initialIdGeneratorContainer

  test("Adding a GameStart action to an initial game state") {
    val collector = ImmutableActionCollector(GameState.empty)
    val gameStart = GameStart(idGen.actionId(), 1)
    val endGame   = EndGame(idGen.actionId(), 2)

    val (afterGameStart, _, _) = collector.masterAddAndRemoveActions(Vector(gameStart))

    assert(afterGameStart.currentGameState.started)
    assert(afterGameStart.actionsAndStates.nonEmpty)
    assertEquals(afterGameStart.actionsAndStates.head._2, Vector[GameAction](gameStart))

    val (afterGameEnds, _, _) = afterGameStart.masterAddAndRemoveActions(Vector(endGame))
    assert(afterGameEnds.currentGameState.ended)
  }

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

    assertEquals(
      collectorAfterActions.currentGameState.castingEntityInfo.keys.toVector,
      Vector(playerId)
    )
  }

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

    val afterGameStart    = collector.masterAddAndRemoveActions(Vector(gameStart))._1
    val afterFirstPlayer  = afterGameStart.masterAddAndRemoveActions(Vector(firstPlayer))._1
    val afterSecondPlayer = afterFirstPlayer.masterAddAndRemoveActions(Vector(secondPlayer))._1

    assertEquals(
      afterSecondPlayer.actionsAndStates.head._2,
      Vector[GameAction](gameStart, secondPlayer, firstPlayer)
    )
  }

  test("Adding two players makes gameState with two players") {
    val collector = ImmutableActionCollector(GameState.empty)
    val gameStart = GameStart(idGen.actionId(), 1)
    idGen.entityId()
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

    val afterGameStart   = collector.masterAddAndRemoveActions(Vector(gameStart))._1
    val afterFirstPlayer = afterGameStart.masterAddAndRemoveActions(Vector(firstPlayer))._1
    assertEquals(afterFirstPlayer.currentGameState.players.size, 1)
    val afterSecondPlayer = afterFirstPlayer.masterAddAndRemoveActions(Vector(secondPlayer))._1
    assertEquals(afterSecondPlayer.currentGameState.players.size, 2)
  }

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

    val withActions = collector.masterAddAndRemoveActions(actions)._1
    assertEquals(
      withActions.currentGameState.players.keys.toVector.sorted,
      Vector(firstPlayerId, secondPlayerId)
    )
  }

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

    val afterDummies = someDummyUpdates.foldLeft(collector) { (newCollector, action) =>
      newCollector.masterAddAndRemoveActions(Vector(action))._1
    }
    val afterGameStart    = afterDummies.masterAddAndRemoveActions(Vector(gameStart))._1
    val afterSecondPlayer = afterGameStart.masterAddAndRemoveActions(Vector(secondPlayer))._1
    val afterFirstPlayer  = afterSecondPlayer.masterAddAndRemoveActions(Vector(firstPlayer))._1

    // only first player should be there
    assertEquals(afterFirstPlayer.actionsAndStates.head._2, Vector[GameAction](firstPlayer))
  }

  test("Adding one player and starting game together") {
    val collector     = ImmutableActionCollector(GameState.empty)
    val gameStart     = GameStart(idGen.actionId(), 2)
    val firstPlayerId = idGen.entityId()
    val firstPlayer =
      AddPlayerByClass(idGen.actionId(), 1, firstPlayerId, 0, PlayerClasses.Square, 0, "square")

    val (after, _, _) = collector.masterAddAndRemoveActions(Vector(firstPlayer, gameStart))

    assertEquals(after.currentGameState.players.size, 1)
  }
}
