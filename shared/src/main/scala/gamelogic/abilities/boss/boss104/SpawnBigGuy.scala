package gamelogic.abilities.boss.boss104

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.entities.Resource.ResourceAmount
import gamelogic.entities.boss.dawnoftime.Boss104
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.boss104.AddBigGuy
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer

final case class SpawnBigGuy(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability {
  override def abilityId: AbilityId = Ability.boss104SpawnBigGuy

  override def cooldown: Long = SpawnBigGuy.cooldown

  override def castingTime: Long = SpawnBigGuy.castingTime

  override def cost: Resource.ResourceAmount = ResourceAmount(0, Resource.NoResource)

  override def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    // the big guy will be added a bit above the boss when the boss is below the x-axis, and below the boss when
    // it is above the x-axis
    gameState.allTEntities[Boss104].headOption match {
      case None => Vector.empty // will not happen
      case Some((_, boss)) =>
        val deltaFromBoss = boss.shape.radius * 2 * Complex.i
        val bigGuyPosition =
          if boss.pos.im > 0 then boss.pos - deltaFromBoss else boss.pos + deltaFromBoss

        Vector(
          AddBigGuy(genActionId(), time, genEntityId(), genBuffId(), genBuffId(), bigGuyPosition)
        )
    }

  override def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  override def canBeCast(gameState: GameState, time: Long): Option[String] = None
}

object SpawnBigGuy {
  inline def cooldown: Long = 20000

  inline def castingTime: Long = 2000

  inline def timeToFirstUse: Long = 5000
}
