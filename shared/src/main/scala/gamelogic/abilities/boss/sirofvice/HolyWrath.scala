package gamelogic.abilities.boss.sirofvice

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.gameactions.sirofvice.PutHolyWrathChain
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Hits the target, then jumps to the closest player not hit yet, if they are closer than
  * [[HolyWrath.jumpRange]], and so on. Each jump deals more damage than the previous one, so
  * players must spread out.
  */
final case class HolyWrath(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetId: Entity.Id
) extends WithTargetAbility
    with AbilityInfoFromMetadata[HolyWrath.type] {
  def range: Distance = Double.MaxValue

  def metadata = HolyWrath

  def cost: Resource.ResourceAmount = ResourceAmount(0, NoResource)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] = {
    val hitPlayers = HolyWrath.chain(gameState, targetId, time)

    val damages = hitPlayers.zipWithIndex.map { (player, jump) =>
      EntityTakesDamage(genActionId(), time, player.id, HolyWrath.damageForJump(jump), casterId)
    }

    val casterPosition = gameState.withPositionEntityById(casterId).map(_.currentPosition(time))
    val chainPositions = casterPosition.toVector ++ hitPlayers.map(_.currentPosition(time))

    Option
      .when(hitPlayers.nonEmpty)(
        PutHolyWrathChain(genActionId(), time, genEntityId(), chainPositions)
      )
      .toVector ++ damages
  }

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): HolyWrath =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): None.type = None
}

object HolyWrath extends AbilityMetadata {

  val cooldown: Long           = 25000L
  val castingTime: Long        = 1000L
  val timeToFirstAbility: Long = 12000L

  def name: String = "Holy Wrath"

  def abilityId: AbilityId = Ability.sirOfViceHolyWrath

  /** Maximum distance between two players for the wrath to jump from one to the other. */
  inline def jumpRange: Double = 100.0

  inline def baseDamage: Double       = 20.0
  inline def damageMultiplier: Double = 1.5

  /** Damage dealt to the player hit after `jump` jumps (0 for the initial target). */
  def damageForJump(jump: Int): Double = baseDamage * math.pow(damageMultiplier, jump)

  /** The players hit by a wrath starting on `firstTargetId`, in the order they are hit. */
  def chain(gameState: GameState, firstTargetId: Entity.Id, time: Long): Vector[PlayerClass] = {
    @scala.annotation.tailrec
    def jump(hit: Vector[PlayerClass], remaining: Vector[PlayerClass]): Vector[PlayerClass] = {
      val lastPosition = hit.last.currentPosition(time)
      remaining
        .map(player => player -> player.currentPosition(time).distanceTo(lastPosition))
        .filter(_._2 < jumpRange)
        .minByOption(_._2)(using Ordering.Double.TotalOrdering) match {
        case None => hit
        case Some((next, _)) => jump(hit :+ next, remaining.filterNot(_.id == next.id))
      }
    }

    gameState.players.get(firstTargetId).fold(Vector.empty[PlayerClass]) { firstTarget =>
      jump(Vector(firstTarget), gameState.players.values.toVector.filterNot(_.id == firstTargetId))
    }
  }
}
