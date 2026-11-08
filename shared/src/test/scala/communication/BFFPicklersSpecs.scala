package communication

import boopickle.Default.*
import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss104.{
  BigGuyKick,
  DeathMarkTriangleAttack,
  SpawnBigGuy,
  TwinDebuffs
}
import gamelogic.abilities.boss.boss105.{Resonance, Reunion, Siphon, Skewer, VolatileMark}
import gamelogic.abilities.boss.boss110.{
  ExplodeBombs,
  PlaceBombPods,
  SpawnBigGuies,
  SpawnSmallGuies
}
import gamelogic.abilities.boss.boss110.addsabilities.{CreepingShadowTick, PutBrokenArmor}
import gamelogic.abilities.boss.sirofvice.{
  CleansingNova,
  HolyFlame,
  HolyWrath,
  Punishment,
  SacredGround
}
import gamelogic.abilities.boss.spiritofnara.{
  ArcaneExplosion,
  ArcaneMissiles,
  Blizzard,
  ChainsOfIce,
  Counterspell,
  Fireball,
  FlameWreath,
  Frostbolt,
  MagicSchool,
  MagneticPull,
  MassPolymorph,
  Pyroblast,
  ShadowPyroblast,
  SummonWaterElementals,
  Waterbolt
}
import gamelogic.abilities.pentagon.{Fracture, Pentagram}
import gamelogic.abilities.square.{Brace, Guard, ShieldSlam}
import gamelogic.buffs.Buff
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.{PutStackedBuff, RefreshMovementSpeed}
import gamelogic.gamestate.gameactions.classes.pentagon.PutFractured
import gamelogic.gamestate.gameactions.classes.square.PutGuarding
import gamelogic.gamestate.gameactions.boss104.*
import gamelogic.gamestate.gameactions.boss105.{
  AddShade,
  PutVolatileCharge,
  PutYearning,
  ShadeFalls
}
import gamelogic.gamestate.gameactions.boss110.{
  AddBigGuy as Boss110AddBigGuy,
  AddBombPods,
  AddCreepingShadow,
  AddSmallGuy
}
import gamelogic.gamestate.gameactions.sirofvice.{
  PutHolyWrathChain,
  PutInflamedDebuff,
  PutPunishedDebuff,
  PutPurifiedDebuff
}
import gamelogic.gamestate.gameactions.spiritofnara.{
  AddBlizzard,
  AddFlameWreath,
  AddWaterElemental,
  PullPlayer,
  PutChained,
  PutChilled,
  PutCounterspelled,
  PutDrinking,
  PutPolymorph,
  PutSchoolLockout
}
import gamelogic.physics.{Complex, PosInt}
import utils.misc.RGBColour

/** Every [[Ability]] and every [[GameAction]] must be declared in [[BFFPicklers]], otherwise the
  * game blows up the first time it travels on the in-game websocket -- at runtime, and only for
  * that one boss. These round trips are the cheap way to catch a forgotten `addConcreteType`.
  */
final class BFFPicklersSpecs extends munit.FunSuite {

  import BFFPicklers.{abilityPickler, gameActionPickler}

  def roundTripAbility(ability: Ability): Unit =
    assertEquals(Unpickle[Ability].fromBytes(Pickle.intoBytes[Ability](ability)), ability)

  def roundTripAction(action: GameAction): Unit =
    assertEquals(Unpickle[GameAction].fromBytes(Pickle.intoBytes[GameAction](action)), action)

  val entityId: Entity.Id     = Entity.Id.zero
  val buffId: Buff.Id         = Buff.Id.zero
  val actionId: GameAction.Id = GameAction.Id.zero
  val useId: Ability.UseId    = Ability.UseId.zero
  val somewhere: Complex      = Complex(1, 2)

  test("Boss104 abilities survive a boopickle round trip") {
    roundTripAbility(TwinDebuffs(useId, 1L, entityId))
    roundTripAbility(SpawnBigGuy(useId, 1L, entityId))
    roundTripAbility(BigGuyKick(useId, 1L, entityId, entityId))
    roundTripAbility(DeathMarkTriangleAttack(useId, 1L, entityId))
  }

  test("Boss104 actions survive a boopickle round trip") {
    roundTripAction(
      PutTwinDebuff(actionId, 1L, buffId, entityId, entityId, RGBColour.green, entityId, somewhere)
    )
    roundTripAction(AddBigGuy(actionId, 1L, entityId, buffId, buffId, somewhere))
    roundTripAction(AddBigGuyDeathMark(actionId, 1L, entityId, somewhere))
    roundTripAction(
      AddDeathMarkTriangle(actionId, 1L, entityId, Vector[Complex](-150, 150, Complex(0, 150)))
    )
    roundTripAction(DeathMarkTriangleNextStage(actionId, 1L, entityId))
  }

  test("Sir of Vice abilities survive a boopickle round trip") {
    roundTripAbility(CleansingNova(useId, 1L, entityId))
    roundTripAbility(HolyFlame(useId, 1L, entityId, entityId))
    roundTripAbility(HolyWrath(useId, 1L, entityId, entityId))
    roundTripAbility(Punishment(useId, 1L, entityId))
    roundTripAbility(SacredGround(useId, 1L, entityId, somewhere, 10.0))
  }

  test("Sir of Vice actions survive a boopickle round trip") {
    roundTripAction(PutInflamedDebuff(actionId, 1L, buffId, entityId, entityId))
    roundTripAction(PutHolyWrathChain(actionId, 1L, entityId, Vector[Complex](0, somewhere)))
    roundTripAction(PutPunishedDebuff(actionId, 1L, buffId, entityId, entityId, somewhere))
    roundTripAction(PutPurifiedDebuff(actionId, 1L, buffId, entityId, entityId))
  }

  test("Boss105 abilities survive a boopickle round trip") {
    roundTripAbility(Resonance(useId, 1L, entityId))
    roundTripAbility(Skewer(useId, 1L, entityId, entityId))
    roundTripAbility(VolatileMark(useId, 1L, entityId))
    roundTripAbility(Siphon(useId, 1L, entityId, entityId))
    roundTripAbility(Reunion(useId, 1L, entityId))
  }

  test("Boss105 actions survive a boopickle round trip") {
    roundTripAction(
      AddShade(actionId, 1L, entityId, somewhere, entityId, buffId, buffId, buffId)
    )
    roundTripAction(ShadeFalls(actionId, 1L, entityId, buffId, entityId, somewhere))
    roundTripAction(PutVolatileCharge(actionId, 1L, buffId, entityId, entityId))
    roundTripAction(PutYearning(actionId, 1L, buffId, entityId, entityId))
  }

  test("Boss110 abilities survive a boopickle round trip") {
    roundTripAbility(SpawnBigGuies(useId, 1L, entityId))
    roundTripAbility(SpawnSmallGuies(useId, 1L, entityId))
    roundTripAbility(PlaceBombPods(useId, 1L, entityId))
    roundTripAbility(ExplodeBombs(useId, 1L, entityId))
    roundTripAbility(PutBrokenArmor(useId, 1L, entityId, entityId))
    roundTripAbility(CreepingShadowTick(useId, 1L, entityId))
  }

  test("Boss110 actions survive a boopickle round trip") {
    roundTripAction(Boss110AddBigGuy(actionId, 1L, entityId, buffId, somewhere))
    roundTripAction(AddSmallGuy(actionId, 1L, entityId, buffId, somewhere))
    roundTripAction(AddBombPods(actionId, 1L, List(entityId -> somewhere), entityId))
    roundTripAction(AddCreepingShadow(actionId, 1L, entityId, entityId))
  }

  test("The pentagon's abilities and actions survive a boopickle round trip") {
    roundTripAbility(Pentagram(useId, 1L, entityId, entityId))
    roundTripAbility(Fracture(useId, 1L, entityId, somewhere, 50.0, Angle.zero, 0))
    roundTripAction(PutFractured(actionId, 1L, buffId, entityId, entityId, 12000L))
    roundTripAction(
      PutStackedBuff(
        actionId,
        1L,
        buffId,
        entityId,
        entityId,
        1L,
        Buff.pentagonFacets,
        PosInt.of(5)
      )
    )
  }

  test("Brace and stacked buffs survive a boopickle round trip") {
    roundTripAbility(Brace(useId, 1L, entityId))
    roundTripAbility(ShieldSlam(useId, 1L, entityId, entityId))
    roundTripAbility(Guard(useId, 1L, entityId, entityId))
    roundTripAction(PutGuarding(actionId, 1L, buffId, entityId, entityId, buffId))
    roundTripAction(
      PutStackedBuff(actionId, 1L, buffId, entityId, entityId, 1L, Buff.squareBrace, PosInt.of(3))
    )
  }

  test("Spirit of Nara abilities survive a boopickle round trip") {
    roundTripAbility(Fireball(useId, 1L, entityId, entityId))
    roundTripAbility(Frostbolt(useId, 1L, entityId, entityId))
    roundTripAbility(ArcaneMissiles(useId, 1L, entityId, entityId))
    roundTripAbility(Counterspell(useId, 1L, entityId))
    roundTripAbility(ChainsOfIce(useId, 1L, entityId, entityId))
    roundTripAbility(MagneticPull(useId, 1L, entityId))
    roundTripAbility(ArcaneExplosion(useId, 1L, entityId))
    roundTripAbility(FlameWreath(useId, 1L, entityId))
    roundTripAbility(Blizzard(useId, 1L, entityId))
    roundTripAbility(MassPolymorph(useId, 1L, entityId))
    roundTripAbility(Pyroblast(useId, 1L, entityId))
    roundTripAbility(SummonWaterElementals(useId, 1L, entityId, 2))
    roundTripAbility(Waterbolt(useId, 1L, entityId, entityId))
    roundTripAbility(ShadowPyroblast(useId, 1L, entityId))
  }

  test("Spirit of Nara actions survive a boopickle round trip") {
    roundTripAction(PutChilled(actionId, 1L, buffId, entityId))
    roundTripAction(RefreshMovementSpeed(actionId, 1L, entityId))
    roundTripAction(PullPlayer(actionId, 1L, entityId, somewhere, buffId, buffId))
    roundTripAction(PutChained(actionId, 1L, buffId, entityId, somewhere))
    roundTripAction(PutPolymorph(actionId, 1L, buffId, entityId, entityId, somewhere))
    roundTripAction(PutCounterspelled(actionId, 1L, buffId, entityId))
    roundTripAction(PutDrinking(actionId, 1L, buffId, entityId))
    roundTripAction(PutSchoolLockout(actionId, 1L, buffId, entityId, MagicSchool.frost))
    roundTripAction(
      AddFlameWreath(
        actionId,
        1L,
        buffId,
        entityId,
        Vector(entityId, entityId),
        Vector[Complex](0, somewhere),
        Vector(Vector(entityId), Vector.empty)
      )
    )
    roundTripAction(AddBlizzard(actionId, 1L, entityId, Angle.Pi, buffId, entityId))
    roundTripAction(AddWaterElemental(actionId, 1L, entityId, somewhere, buffId, buffId))
  }

}
