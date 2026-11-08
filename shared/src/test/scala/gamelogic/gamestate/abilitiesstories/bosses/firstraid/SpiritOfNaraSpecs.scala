package gamelogic.gamestate.abilitiesstories.bosses.firstraid

import gamelogic.abilities.Ability
import gamelogic.abilities.boss.spiritofnara.*
import gamelogic.abilities.triangle.Cut
import gamelogic.buffs.SilencingBuff
import gamelogic.buffs.boss.spiritofnara.*
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.firstraid.SpiritOfNara
import gamelogic.entities.boss.spiritofnara.FlameWreathRing
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.abilitiesstories.StoryTeller
import gamelogic.gamestate.gameactions.*
import gamelogic.gamestate.gameactions.spiritofnara.AddFlameWreath
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.VisibilityGraph
import models.bff.outofgame.PlayerClasses
import testutils.ActionComposer

final class SpiritOfNaraSpecs extends StoryTeller {

  val room: GameState = initialGameState.applyActions(SpiritOfNara.stagingBossActions(0))

  def spawnBoss(time: Long): SpawnBoss =
    SpawnBoss(genActionId(), time, genEntityId(), SpiritOfNara.name)

  def addPlayerAt(
      time: Long,
      position: Complex,
      cls: PlayerClasses = PlayerClasses.Pentagon
  ): AddPlayerByClass =
    AddPlayerByClass(genActionId(), time, genEntityId(), position, cls, 0, s"Player$time")

  /** The actions as the game master would apply them, i.e. after the passive buffs had their say.
    */
  def throughBuffs(actions: Vector[GameAction])(gameState: GameState): Vector[GameAction] =
    gameState.applyActionChangers(actions.toList).toVector

  def bossOf(gs: GameState): SpiritOfNara = gs.bosses.values.collectFirst {
    case boss: SpiritOfNara => boss
  }.get

  test("Players can walk from the entrance to anywhere around the boss") {
    val graph = VisibilityGraph(room.obstacles.values, Constants.playerRadius)
    assertEquals(room.obstacles.size, SpiritOfNara.numberOfWalls)
    for k <- 0 until 12 do {
      val spot = Complex.polar(ArcaneExplosion.radius + 40, Angle.fromDouble(k * math.Pi / 6))
      assert(graph.isLegal(spot), s"$spot is not a legal position")
      assert(graph.findPath(SpiritOfNara.playersStartingPosition, spot).isDefined)
    }
  }

  test("Cutting a spell locks the boss out of its school, instead of silencing it") {
    val boss     = spawnBoss(1)
    val triangle = addPlayerAt(2, Complex(-60, 0), PlayerClasses.Triangle)
    val fireball = Fireball(Ability.UseId.zero, 1000, boss.entityId, triangle.entityId)

    val composer = ActionComposer.empty >> start >> boss >> triangle >>
      EntityStartsCasting(genActionId(), 1000, Fireball.castingTime, fireball) >>>
      (gs =>
        Cut(Ability.UseId.zero, 1500, triangle.entityId, boss.entityId).createActions(gs)
      ) >>>> { gs =>
        assert(!gs.entityIsCasting(boss.entityId))
        assert(SchoolLockout.isLocked(gs, boss.entityId, MagicSchool.fire))
        assert(!SchoolLockout.isLocked(gs, boss.entityId, MagicSchool.frost))
        assert(!gs.hasBuffOfType[SilencingBuff](boss.entityId))
      }

    composer(room)
  }

  test("A Frostbolt slows its target down, until the chill ends") {
    val boss   = spawnBoss(1)
    val player = addPlayerAt(2, Complex(-300, 0))
    val speed  = Constants.playerSpeed

    def playerOf(gs: GameState) = gs.players(player.entityId)

    val composer = ActionComposer.empty >> start >> boss >> player >>
      entityMoves(
        1000,
        player.entityId,
        Complex(-300, 0),
        Angle.Pi / 2,
        Angle.zero,
        speed,
        true
      ) >>>
      (gs =>
        Frostbolt(Ability.UseId.zero, 2000, boss.entityId, player.entityId).createActions(gs)
      ) >>>> { gs =>
        // slowed down right away, from where it was at that time
        assertEqualsDouble(playerOf(gs).speed, speed * Chilled.speedMultiplier, 1e-9)
        assert(playerOf(gs).pos.distanceTo(Complex(-300, speed)) < 1e-6)
      } >>
      // the player claims it goes at full speed, which it can't
      entityMoves(
        2500,
        player.entityId,
        Complex(-300, 200),
        Angle.zero,
        Angle.zero,
        speed,
        true
      ) >>>> { gs =>
        assertEqualsDouble(playerOf(gs).speed, speed * Chilled.speedMultiplier, 1e-9)
      } >>>
      (gs =>
        gs.allTBuffs[Chilled]
          .toVector
          .flatMap(buff =>
            RemoveBuff(genActionId(), 7000, buff.bearerId, buff.buffId) +:
              buff.endingAction(gs, 7000, None)
          )
      ) >>>> { gs =>
        assertEqualsDouble(playerOf(gs).speed, speed, 1e-9)
      }

    composer(room)
  }

  test("The Magnetic Pull brings everyone next to the boss, where they stay for a moment") {
    val boss   = spawnBoss(1)
    val player = addPlayerAt(2, Complex(-300, 0))
    val pulled = Complex(-MagneticPull.distance, 0)

    def playerOf(gs: GameState) = gs.players(player.entityId)

    val composer = ActionComposer.empty >> start >> boss >> player >>>
      (gs => MagneticPull(Ability.UseId.zero, 1000, boss.entityId).createActions(gs)) >>>> { gs =>
        assert(playerOf(gs).pos.distanceTo(pulled) < 1e-6)
        assert(gs.hasBuffOfType[MassSlow](player.entityId))
        // the explosion would kill it there
        val hit = ArcaneExplosion(Ability.UseId.zero, 1100, boss.entityId).createActions(gs)
        assertEquals(
          hit.collect { case d: EntityTakesDamage => d.entityId },
          Vector(player.entityId)
        )
      } >>> throughBuffs(
        // the client of the player did not know yet, and keeps moving from where it was
        Vector(
          entityMoves(1200, player.entityId, Complex(-310, 0), Angle.Pi, Angle.Pi, 150, true)
        )
      ) >>>> { gs =>
        assert(playerOf(gs).pos.distanceTo(pulled) < 1e-6)
        assert(!playerOf(gs).moving)
      }

    composer(room)
  }

  test("Crossing a ring of the Flame Wreath, in either direction, makes them all explode") {
    val boss     = spawnBoss(1)
    val centre   = Complex(-200, 0)
    val target   = addPlayerAt(2, centre)
    val neighbor = addPlayerAt(3, centre + Complex(30, 0))
    val outsider = addPlayerAt(4, Complex(200, 0))

    def wreath(gs: GameState) = AddFlameWreath(
      genActionId(),
      1000,
      genBuffId(),
      boss.entityId,
      Vector(genEntityId()),
      Vector(centre),
      Vector(
        gs.players.values
          .filter(_.pos.distanceTo(centre) < FlameWreathRing.radius)
          .map(_.id)
          .toVector
      )
    )

    def watch(gs: GameState) = gs.allTBuffs[FlameWreathWatch].head

    val composer = ActionComposer.empty >> start >> boss >> target >> neighbor >> outsider >>>
      (gs => Vector(wreath(gs))) >>
      // moving inside the ring is fine
      entityMoves(
        1500,
        neighbor.entityId,
        centre + Complex(-40, 20),
        Angle.zero,
        Angle.zero,
        150,
        false
      ) >>>> { gs =>
        assertEquals(watch(gs).tickEffect(gs, 1600), Vector.empty)
      } >>
      // going in is not
      entityMoves(
        2000,
        outsider.entityId,
        centre + Complex(0, 50),
        Angle.zero,
        Angle.zero,
        150,
        false
      ) >>>
      (gs => watch(gs).tickEffect(gs, 2100)) >>>> { gs =>
        assert(gs.allTEntities[FlameWreathRing].isEmpty)
        assert(!gs.hasBuffOfType[FlameWreathWatch](boss.entityId))
        gs.players.values.foreach(player =>
          assertEqualsDouble(player.life, player.maxLife - FlameWreathWatch.explosionDamage, 1e-9)
        )
      }

    composer(room)
  }

  test("Out of mana, the boss polymorphs everyone and drinks, then casts its Pyroblast") {
    val boss   = spawnBoss(1)
    val player = addPlayerAt(2, Complex(-200, 0))
    val poly   = MassPolymorph(Ability.UseId.zero, 1000, boss.entityId)

    val composer = ActionComposer.empty >> start >> boss >> player >>
      EntityTakesDamage(genActionId(), 500, player.entityId, 60, boss.entityId) >>>> { gs =>
        assert(!bossOf(gs).pyroblastPending(gs))
      } >>
      useAbilityFromAbility(poly) >>> poly.createActions >>>> { gs =>
        assert(gs.hasBuffOfType[Polymorphed](player.entityId))
        assert(bossOf(gs).isDrinking(gs))
        // no pyroblast while drinking
        assert(!bossOf(gs).pyroblastPending(gs))
      } >>> throughBuffs(
        Vector(entityMoves(1200, player.entityId, Complex(0, 0), Angle.zero, Angle.zero, 150, true))
      ) >>>> { gs =>
        assert(gs.players(player.entityId).pos.distanceTo(Complex(-200, 0)) < 1e-6)
      } >>>
      (gs =>
        gs.allBuffs.toVector
          .collect { case buff: (Drinking | Polymorphed) => buff }
          .flatMap(buff =>
            RemoveBuff(genActionId(), 11000, buff.bearerId, buff.buffId) +:
              buff.endingAction(gs, 11000, None)
          )
      ) >>>> { gs =>
        // back to full life, and the boss is done drinking
        assertEqualsDouble(
          gs.players(player.entityId).life,
          gs.players(player.entityId).maxLife,
          1e-9
        )
        assert(bossOf(gs).pyroblastPending(gs))
      } >>
      useAbilityFromAbility(Pyroblast(Ability.UseId.zero, 14000, boss.entityId)) >>>> { gs =>
        assert(!bossOf(gs).pyroblastPending(gs))
      }

    composer(room)
  }

  test("Water Elementals come in a wave under 40% of the boss' life, and another under 20%") {
    val boss = SpiritOfNara.initialBoss(genEntityId(), 0)
    def at(lifeRatio: Double, lastWave: Int) = boss.copy(
      life = boss.maxLife * lifeRatio,
      relevantUsedAbilities = boss.relevantUsedAbilities + (
        Ability.spiritOfNaraSummonElementals ->
          SummonWaterElementals(Ability.UseId.zero, 0, boss.id, lastWave)
      )
    )

    assertEquals(boss.dueElementalWave, None)
    assertEquals(at(0.5, 0).dueElementalWave, None)
    assertEquals(at(0.4, 0).dueElementalWave, Some(1))
    assertEquals(at(0.3, 1).dueElementalWave, None)
    assertEquals(at(0.1, 1).dueElementalWave, Some(2))
    assertEquals(at(0.1, 2).dueElementalWave, None)
  }

  test("The boss starts with full mana, and no explosion or pyroblast pending") {
    val boss = SpiritOfNara.initialBoss(genEntityId(), 0)
    assertEqualsDouble(boss.resourceAmount.amount, SpiritOfNara.maxMana, 1e-9)
    assert(!boss.arcaneExplosionPending)
    assert(!boss.needsToDrink)
    assertEquals(boss.lastSpecial, -SpiritOfNara.specialInterval)
  }

}
