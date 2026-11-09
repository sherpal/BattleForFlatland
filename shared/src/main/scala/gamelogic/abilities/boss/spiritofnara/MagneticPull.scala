package gamelogic.abilities.boss.spiritofnara

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.classes.Constants
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.spiritofnara.PullPlayer
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer
import services.i18n.I18n

/** First half of the Arcane Explosion: every player is pulled right next to the Spirit of Nara, and
  * slowed down. The [[ArcaneExplosion]] itself follows.
  *
  * Players keep the side they were on, so that they know where to run.
  */
final case class MagneticPull(useId: Ability.UseId, time: Long, casterId: Entity.Id)
    extends Ability
    with AbilityInfoFromMetadata[MagneticPull.type] {
  def metadata = MagneticPull

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(MagneticPull.manaCost, Resource.Mana)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    gameState
      .withPositionEntityById(casterId)
      .fold(Vector.empty[GameAction]) { caster =>
        val centre = caster.currentPosition(time)
        gameState.players.values.toVector.sortBy(_.id.value).map { player =>
          val side = (player.currentPosition(time) - centre).safeNormalized
          val direction =
            if side.modulus2 > 0.5 then side else Complex(-1, 0) // right on the boss: any side
          PullPlayer(
            genActionId(),
            time,
            player.id,
            centre + direction * MagneticPull.distance,
            genBuffId(),
            genBuffId()
          )
        }
      }

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): MagneticPull =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long)(using I18n): None.type = None
}

object MagneticPull extends AbilityMetadata {
  val name: String = "Magnetic Pull"

  /** The special abilities come in turn, never twice the same in a row (see the AI). */
  val cooldown: Long           = 60000L
  val castingTime: Long        = 0L
  val timeToFirstAbility: Long = 30000L

  inline def manaCost: Double = 40.0

  /** From the centre of the boss to the centre of the pulled players. */
  inline def distance: Double = Constants.bossRadius + Constants.playerRadius + 10

  def abilityId: AbilityId = Ability.spiritOfNara.magneticPullId
}
