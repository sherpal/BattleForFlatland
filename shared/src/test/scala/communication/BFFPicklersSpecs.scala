package communication

import boopickle.Default.*
import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss104.{
  BigGuyKick,
  DeathMarkTriangleAttack,
  SpawnBigGuy,
  TwinDebuffs
}
import gamelogic.buffs.Buff
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.boss104.*
import gamelogic.physics.Complex
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

}
