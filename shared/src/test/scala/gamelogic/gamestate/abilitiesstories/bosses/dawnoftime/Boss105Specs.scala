package gamelogic.gamestate.abilitiesstories.bosses.dawnoftime

import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss105.{Resonance, Skewer, VolatileMark}
import gamelogic.abilities.pentagon.PentaDispel
import gamelogic.buffs.boss.boss105.{Mourning, ShadeBond, TwinBond, VolatileCharge}
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss105.{ReformationPoint, Shade}
import gamelogic.entities.boss.dawnoftime.Boss105
import gamelogic.gamestate.abilitiesstories.StoryTeller
import gamelogic.gamestate.gameactions.boss105.{AddShade, PutVolatileCharge}
import gamelogic.gamestate.gameactions.{
  AddPlayerByClass,
  EntityTakesDamage,
  RemoveEntity,
  SpawnBoss
}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import models.bff.outofgame.PlayerClasses
import testutils.ActionComposer

/** Stories of the mechanics of Boss105: the bond between the twins, the death of the Shade and its
  * return, the beam, and the volatile charge.
  */
final class Boss105Specs extends StoryTeller {

  val bossId: Entity.Id = genEntityId()

  val room: GameState = initialGameState.applyActions(Boss105.stagingBossActions(0))

  val spawnBoss: SpawnBoss = SpawnBoss(genActionId(), 1, bossId, Boss105.name)

  def addPlayerAt(
      time: Long,
      position: Complex,
      cls: PlayerClasses = PlayerClasses.Square
  ): AddPlayerByClass =
    AddPlayerByClass(genActionId(), time, genEntityId(), position, cls, 0, s"Player$time")

  def theShade(gameState: GameState): Option[Shade] =
    gameState.allTEntities[Shade].values.headOption

  def theBoss(gameState: GameState): Boss105 = gameState.bosses(bossId).asInstanceOf[Boss105]

  /** The fight as it begins: the boss and its Shade, bonded. */
  def withInitialActions: ActionComposer =
    ActionComposer.empty >> start >> spawnBoss >>> (_ => Boss105.initialBossActions(bossId, 2))

  /** Mimics what [[gamelogic.gamestate.serveractions.ManageDeadAIs]] does when the Shade dies: run
    * the death actions of its buffs, then remove it.
    */
  def killShade(time: Long)(gameState: GameState): Vector[GameAction] = {
    val shadeId = theShade(gameState).get.id
    gameState
      .allBuffsOfEntity(shadeId)
      .flatMap(_.bearerDiedAction(gameState, time))
      .toVector :+ RemoveEntity(genActionId(), time, shadeId)
  }

  /** Mimics what [[gamelogic.gamestate.serveractions.ManageBuffsToBeRemoved]] does for the mourning
    * of the boss when it ends.
    */
  def endMourning(time: Long)(gameState: GameState): Vector[GameAction] =
    gameState
      .allBuffsOfEntity(bossId)
      .collect { case mourning: Mourning => mourning }
      .flatMap(mourning =>
        gamelogic.gamestate.gameactions
          .RemoveBuff(genActionId(), time, bossId, mourning.buffId) +:
          mourning.endingAction(gameState, time, None)
      )
      .toVector

  def damageToBoss(amount: Double, time: Long): EntityTakesDamage =
    EntityTakesDamage(genActionId(), time, bossId, amount, Entity.Id.zero)

  def damagesByEntity(actions: Vector[GameAction]): Map[Entity.Id, Double] =
    actions.collect { case damage: EntityTakesDamage => damage.entityId -> damage.amount }.toMap

  test("The boss starts the fight with its Shade, far from it, and takes little damage") {
    val composer = withInitialActions >>>> { (gs: GameState) =>
      val shade = theShade(gs).get
      assert(gs.hasBuffOfType[TwinBond](bossId))
      assert(gs.hasBuffOfType[ShadeBond](shade.id))
      assert(
        shade.pos.distanceTo(theBoss(gs).pos) > 2 * Resonance.range,
        "the twins must not start in resonance"
      )

      val reducedDamage = gs
        .applyActionChangers(damageToBoss(100, 3))
        .collect { case damage: EntityTakesDamage if damage.entityId == bossId => damage.amount }
      assertEquals(reducedDamage, List(100 * TwinBond.damageMultiplier))
    }

    composer(room)
  }

  test("Resonance is only possible when the twins are close to each other") {
    def resonanceAt(time: Long)(gs: GameState) =
      Resonance(Ability.UseId.zero, time, bossId).canBeCast(gs, time)

    val composer = withInitialActions >> addPlayerAt(3, Complex(0, 0)) >>>> { (gs: GameState) =>
      assert(resonanceAt(10)(gs).isDefined)
    } >>> { (gs: GameState) =>
      // drops the Shade on the boss
      val shade = theShade(gs).get
      Vector(
        RemoveEntity(genActionId(), 4, shade.id),
        AddShade(
          genActionId(),
          5,
          shade.id,
          theBoss(gs).pos + Complex(Resonance.range / 2, 0),
          bossId,
          genBuffId(),
          genBuffId(),
          genBuffId()
        )
      )
    } >>>> { (gs: GameState) =>
      assert(resonanceAt(10)(gs).isEmpty)
      val damages = Resonance(Ability.UseId.zero, 10, bossId).createActions(gs)
      assertEquals(damagesByEntity(damages).values.toSet, Set(Resonance.damage))
      assertEquals(damages.length, gs.players.size)
    }

    composer(room)
  }

  test("When the Shade falls, the boss is exposed until the Shade comes back far from it") {
    val composer = withInitialActions >>> killShade(10) >>>> { (gs: GameState) =>
      assertEquals(theShade(gs), None)
      assert(!gs.hasBuffOfType[TwinBond](bossId))
      assert(gs.hasBuffOfType[Mourning](bossId))
      assert(theBoss(gs).isMourning(gs))

      // the boss takes the full damage...
      assertEquals(
        gs.applyActionChangers(damageToBoss(100, 11))
          .collect { case damage: EntityTakesDamage => damage.amount },
        List(100.0)
      )
      // ... and hits harder and faster
      val frenzied = theBoss(gs).copy(targetId = bossId).maybeAutoAttack(11, gs)
      assert(frenzied.forall(_.damage == Boss105.frenziedAutoAttackDamage))

      // the Shade will come back at the marked spot
      val marker = gs.allTEntities[ReformationPoint].values.head
      assertEquals(marker.pos, Shade.reformationPosition(Boss105.size, theBoss(gs).pos))
    } >>> endMourning(10 + Mourning.duration) >>>> { (gs: GameState) =>
      val shade = theShade(gs).get
      assert(gs.hasBuffOfType[TwinBond](bossId))
      assert(gs.hasBuffOfType[ShadeBond](shade.id))
      assert(!gs.hasBuffOfType[Mourning](bossId))
      assertEquals(gs.allTEntities[ReformationPoint].size, 0)
      assert(shade.pos.distanceTo(theBoss(gs).pos) > Resonance.range)
    }

    composer(room)
  }

  test("The Shade comes back where the boss is not") {
    // whatever the corner the boss is closest to, the Shade comes back from the opposite one
    Vector(Complex(-300, -300), Complex(300, 300), Complex(-300, 300), Complex(300, -300)).foreach {
      bossPosition =>
        val reformation = Shade.reformationPosition(Boss105.size, bossPosition)
        assert(reformation.distanceTo(bossPosition) > 2 * Resonance.range)
    }
  }

  test("Skewer hits the players on the beam, and only them") {
    val source  = Complex(-250, 0)
    val through = Complex(0, 0)

    assert(Skewer.isOnBeam(Complex(100, 0), source, through), "on the beam, behind the target")
    assert(Skewer.isOnBeam(Complex(-100, Skewer.halfWidth - 1), source, through), "on the edge")
    assert(!Skewer.isOnBeam(Complex(-100, Skewer.halfWidth + 1), source, through), "next to it")
    assert(!Skewer.isOnBeam(Complex(-300, 0), source, through), "behind the boss")

    val target   = addPlayerAt(3, through)
    val behind   = addPlayerAt(4, Complex(200, 5))
    val between  = addPlayerAt(5, Complex(-100, -5))
    val aside    = addPlayerAt(6, Complex(-100, 100))
    val behindIt = addPlayerAt(7, Complex(-400, 0))

    val composer = ActionComposer.empty >> start >> spawnBoss >>
      target >> behind >> between >> aside >> behindIt >>>> { (gs: GameState) =>
        val bossAtOrigin = gs.entities + (bossId -> theBoss(gs).copy(pos = source))
        val hit = Skewer(Ability.UseId.zero, 10, bossId, target.entityId)
          .createActions(gs.copy(entities = bossAtOrigin))
        assertEquals(
          damagesByEntity(hit).keySet,
          Set(target.entityId, behind.entityId, between.entityId)
        )
        assertEquals(damagesByEntity(hit).values.toSet, Set(Skewer.damage))
      }

    composer(room)
  }

  test("A volatile charge explodes on those next to the bearer, unless it is dispelled") {
    val bearer    = addPlayerAt(3, Complex(0, 0))
    val neighbour = addPlayerAt(4, Complex(VolatileCharge.radius - 10, 0))
    val farAway   = addPlayerAt(5, Complex(VolatileCharge.radius + 50, 0))

    def charge(gs: GameState): VolatileCharge =
      gs.allTBuffs[VolatileCharge].head

    val composer = ActionComposer.empty >> start >> spawnBoss >> bearer >> neighbour >> farAway >>
      PutVolatileCharge(genActionId(), 6, genBuffId(), bearer.entityId, bossId) >>>> {
        (gs: GameState) =>
          assert(charge(gs).canBeDispelled)

          // left alone, it explodes
          val explosion = charge(gs).endingAction(gs, 6 + VolatileCharge.duration, None)
          assertEquals(
            damagesByEntity(explosion).keySet,
            Set(bearer.entityId, neighbour.entityId)
          )
          assertEquals(damagesByEntity(explosion).values.toSet, Set(VolatileCharge.damage))

          // a dispel is harmless
          val dispelled = charge(gs).endingAction(gs, 10, Some(farAway.entityId))
          assertEquals(dispelled, Vector.empty)
      } >>> (gs =>
        PentaDispel(Ability.UseId.zero, 10, farAway.entityId, bearer.entityId).createActions(gs)
      ) >>>> { (gs: GameState) =>
        assert(!gs.hasBuffOfType[VolatileCharge](bearer.entityId))
      }

    composer(room)
  }

  test("The volatile mark charges two different players") {
    val players = (0 until 4).toVector.map(j => addPlayerAt(3 + j, Complex(50 * j, 0)))

    val composer = players.foldLeft(ActionComposer.empty >> start >> spawnBoss)(_ >> _) >>>
      (gs => VolatileMark(Ability.UseId.zero, 20, bossId).createActions(gs)) >>>> {
        (gs: GameState) =>
          val bearers = gs.allTBuffs[VolatileCharge].map(_.bearerId).toSet
          assertEquals(bearers.size, VolatileMark.numberOfBearers)
      }

    composer(room)
  }
}
