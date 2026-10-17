package gamelogic.gamestate.abilitiesstories.bosses.firstraid

import gamelogic.abilities.Ability
import gamelogic.abilities.boss.sirofvice.{CleansingNova, HolyWrath, Punishment, SacredGround}
import gamelogic.abilities.hexagon.FlashHeal
import gamelogic.buffs.boss.sirofvice.{Punished, Purified}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.firstraid.SirOfVice
import gamelogic.entities.boss.sirofvice.HolyWrathChain
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.abilitiesstories.StoryTeller
import gamelogic.gamestate.gameactions.{
  AddPlayerByClass,
  EntityStartsCasting,
  EntityTakesDamage,
  SpawnBoss
}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.VisibilityGraph
import gamelogic.physics.shape.Shape
import models.bff.outofgame.PlayerClasses
import testutils.ActionComposer

final class SirOfViceSpecs extends StoryTeller {

  val room: GameState = initialGameState.applyActions(SirOfVice.stagingBossActions(0))

  def spawnBoss(time: Long): SpawnBoss =
    SpawnBoss(genActionId(), time, genEntityId(), SirOfVice.name)

  def addPlayerAt(
      time: Long,
      position: Complex,
      cls: PlayerClasses = PlayerClasses.Square
  ): AddPlayerByClass =
    AddPlayerByClass(genActionId(), time, genEntityId(), position, cls, 0, s"Player$time")

  /** The actions as the game master would apply them, i.e. after the passive buffs had their say.
    */
  def throughBuffs(actions: Vector[GameAction])(gameState: GameState): Vector[GameAction] =
    gameState.applyActionChangers(actions.toList).toVector

  test("The room has six pillars and every obstacle triangulates properly") {
    assertEquals(room.obstacles.size, 12)
    room.obstacles.values.foreach { obstacle =>
      val triangulatedArea = obstacle.shape.triangulation.map(_.det / 2).sum
      assertEqualsDouble(
        triangulatedArea,
        math.abs(Shape.signedArea(obstacle.shape.vertices)),
        1e-6
      )
    }
  }

  test("Players and the boss can reach the hiding spots behind every pillar") {
    val playerGraph  = VisibilityGraph(room.obstacles.values, Constants.playerRadius)
    val bossGraph    = VisibilityGraph(room.obstacles.values, Constants.bossRadius)
    val bossPosition = Complex(30, -20) // not exactly in the middle, as in a real fight

    SirOfVice.pillarPositions.foreach { pillar =>
      val hidingSpot = SirOfVice.hidingSpotBehind(pillar, bossPosition)
      assert(playerGraph.isLegal(hidingSpot), s"$hidingSpot is not a legal position")
      assert(playerGraph.findPath(SirOfVice.playersStartingPosition, hidingSpot).isDefined)
      // a boss chasing a player hiding there can get to them
      assert(
        bossGraph.findPath(bossPosition, hidingSpot).exists(_.last.distanceTo(hidingSpot) < 60)
      )
    }
  }

  test("Cleansing Nova only hits players in sight of the boss") {
    val boss = spawnBoss(1)
    val hidden =
      SirOfVice.pillarPositions.map(pillar => addPlayerAt(2, SirOfVice.hidingSpotBehind(pillar, 0)))
    val inTheOpen   = addPlayerAt(3, Complex(-300, 0))
    val nearTheBoss = addPlayerAt(4, Complex(0, 60))

    val composer = hidden.foldLeft(ActionComposer.empty >> start >> boss)(_ >> _) >>
      inTheOpen >> nearTheBoss >>>> { (gs: GameState) =>
        val hitPlayers = CleansingNova(Ability.UseId.zero, 10, boss.entityId)
          .createActions(gs)
          .collect { case damage: EntityTakesDamage => damage.entityId }
          .toSet
        assertEquals(hitPlayers, Set(inTheOpen.entityId, nearTheBoss.entityId))
      }

    composer(room)
  }

  test("Punishment freezes players where they are, until they take damage") {
    val boss   = spawnBoss(1)
    val player = addPlayerAt(2, Complex(-300, 0))
    val speed  = Constants.playerSpeed

    def playerOf(gs: GameState) = gs.players(player.entityId)

    val composer = ActionComposer.empty >> start >> boss >> player >>
      entityMoves(
        1000,
        player.entityId,
        Complex(-300, 0),
        Angle.zero,
        Angle.zero,
        speed,
        moving = true
      ) >>>
      (gs => Punishment(Ability.UseId.zero, 2000, boss.entityId).createActions(gs)) >>>> { gs =>
        // stopped where it was at that time, and not where it started to move
        val frozenAt = Complex(-300 + speed, 0)
        assert(!playerOf(gs).moving)
        assert(playerOf(gs).currentPosition(5000).distanceTo(frozenAt) < 1e-6)
        assert(gs.hasBuffOfType[Punished](player.entityId))
      } >>> throughBuffs(
        Vector(
          entityMoves(
            3000,
            player.entityId,
            Complex(-100, 0),
            Angle.zero,
            Angle.zero,
            speed,
            moving = true
          )
        )
      ) >>>> { gs =>
        // trying to move does nothing
        assert(!playerOf(gs).moving)
        assert(playerOf(gs).pos.distanceTo(Complex(-300 + speed, 0)) < 1e-6)
      } >>> throughBuffs(
        Vector(EntityTakesDamage(genActionId(), 4000, player.entityId, 1, boss.entityId))
      ) >>>> { gs =>
        assert(!gs.hasBuffOfType[Punished](player.entityId))
      }

    composer(room)
  }

  test("Sacred Ground splits its damage and silences mana users") {
    val boss = spawnBoss(1)
    val inside =
      (0 until 3).toVector.map(j => addPlayerAt(2 + j, Complex(50 * j, 0), PlayerClasses.Hexagon))
    val outside = addPlayerAt(10, Complex(-SacredGround.radius - 10, 0), PlayerClasses.Hexagon)

    val composer = inside.foldLeft(ActionComposer.empty >> start >> boss)(_ >> _) >> outside >>>> {
      gs =>
        val actions = SacredGround(Ability.UseId.zero, 20, boss.entityId, 0, SacredGround.radius)
          .createActions(gs)
        val damages = actions.collect { case damage: EntityTakesDamage =>
          damage.entityId -> damage.amount
        }.toMap
        assertEquals(damages.keySet, inside.map(_.entityId).toSet)
        assert(damages.values.forall(_ == (SacredGround.damage / 5).toInt.toDouble))
    } >>> (gs =>
      SacredGround(Ability.UseId.zero, 20, boss.entityId, 0, SacredGround.radius).createActions(gs)
    ) >>>> { gs =>
      assert(inside.forall(player => gs.hasBuffOfType[Purified](player.entityId)))
      assert(!gs.hasBuffOfType[Purified](outside.entityId))

      def flashHealFrom(playerId: gamelogic.entities.Entity.Id) =
        EntityStartsCasting(
          genActionId(),
          30,
          1000L,
          FlashHeal(Ability.UseId.zero, 30, playerId, playerId)
        )
      assert(gs.applyActionChangers(flashHealFrom(inside.head.entityId)).isEmpty)
      assertEquals(gs.applyActionChangers(flashHealFrom(outside.entityId)).length, 1)
    }

    composer(room)
  }

  test("Holy Wrath jumps to close players, harder at each jump, and leaves a single chain") {
    val boss = spawnBoss(1)
    // a line of players, each close enough to the previous one, then one too far away
    val step    = HolyWrath.jumpRange - 10
    val inALine = (0 until 3).toVector.map(j => addPlayerAt(2 + j, Complex(-300 + j * step, 100)))
    val farAway = addPlayerAt(10, Complex(-300 + 2 * step + HolyWrath.jumpRange + 10, 100))
    val firstTarget  = inALine.head.entityId
    val expectedHits = inALine.map(_.entityId)

    def wrathAt(time: Long)(gs: GameState): Vector[GameAction] =
      HolyWrath(Ability.UseId.zero, time, boss.entityId, firstTarget).createActions(gs)

    val composer = inALine.foldLeft(ActionComposer.empty >> start >> boss)(_ >> _) >> farAway >>>> {
      gs =>
        val damages = wrathAt(20)(gs).collect { case damage: EntityTakesDamage =>
          damage.entityId -> damage.amount
        }
        assertEquals(damages.map(_._1), expectedHits)
        assertEquals(damages.map(_._2), Vector(0, 1, 2).map(HolyWrath.damageForJump))
        assert(damages.map(_._2).zip(damages.map(_._2).tail).forall(_ < _))
    } >>> wrathAt(20) >>> wrathAt(30) >>>> { gs =>
      val chains = gs.allTEntities[HolyWrathChain].values.toVector
      assertEquals(chains.length, 1)
      assertEquals(chains.head.time, 30L)
      // from the boss, through the three players
      assertEquals(chains.head.positions.length, 4)
    }

    composer(room)
  }

}
