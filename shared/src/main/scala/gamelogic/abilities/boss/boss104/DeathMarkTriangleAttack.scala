package gamelogic.abilities.boss.boss104

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.entities.Resource.ResourceAmount
import gamelogic.entities.boss.boss104.{BigGuyDeathMark, DeathMarkTriangle}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.{EntityTakesDamage, RemoveEntity}
import gamelogic.gamestate.gameactions.boss104.DeathMarkTriangleNextStage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Boss104's two-stage finisher, available only while a
  * [[gamelogic.entities.boss.boss104.DeathMarkTriangle]] stands in the room.
  *
  * The first time it resolves, everybody inside the triangle is crushed and everybody outside is
  * barely scratched; the triangle then moves to its second stage and the very same ability does the
  * exact opposite. After the second resolution the triangle and the death marks are cleaned up, the
  * ability becomes uncastable again, and the fight resumes its normal course.
  *
  * The long casting time is the whole point: it is the warning players get to run out (then back
  * in).
  */
final case class DeathMarkTriangleAttack(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id
) extends Ability {

  override def abilityId: AbilityId = Ability.boss104DeathMarkTriangleAttack

  override def cooldown: Long = 0L

  override def castingTime: Long = DeathMarkTriangleAttack.castingTime

  override def cost: ResourceAmount = ResourceAmount(0, Resource.NoResource)

  override def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  /** Unlike Boss104's other abilities, this one is not simply gated on its cooldown: it only exists
    * while the death mark triangle does.
    */
  override def canBeCast(gameState: GameState, time: Long): Option[String] =
    Option.unless(gameState.allTEntities[DeathMarkTriangle].nonEmpty)(
      "There is no death mark triangle"
    )

  override def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    gameState.allTEntities[DeathMarkTriangle].values.headOption.fold(Vector.empty) { triangle =>
      // Players are split on their centre being inside or outside, so that the two stages are
      // exact opposites of each other and standing on the edge can't be used to cheat both.
      val damages = gameState.players.values.toVector.map { player =>
        EntityTakesDamage(
          genActionId(),
          time,
          player.id,
          triangle.damageAt(player.currentPosition(time)),
          casterId
        )
      }

      val afterMath =
        if triangle.isLastStage then
          // The mechanic is over: wipe the triangle and the marks it was built on, so that the next
          // three big guys start a fresh one.
          RemoveEntity(genActionId(), time, triangle.id) +:
            gameState
              .allTEntities[BigGuyDeathMark]
              .keys
              .toVector
              .map(RemoveEntity(genActionId(), time, _))
        else Vector(DeathMarkTriangleNextStage(genActionId(), time, triangle.id))

      damages ++ afterMath
    }
}

object DeathMarkTriangleAttack {

  inline def castingTime: Long = 5000

}
