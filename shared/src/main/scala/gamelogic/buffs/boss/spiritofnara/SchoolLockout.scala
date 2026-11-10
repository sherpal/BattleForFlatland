package gamelogic.buffs.boss.spiritofnara

import gamelogic.abilities.boss.spiritofnara.MagicSchool
import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** The bearer can't cast spells of the given [[MagicSchool]] (it has been cut while casting one).
  *
  * The buff itself prevents nothing: the AI of the Spirit of Nara reads it.
  */
final case class SchoolLockout(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    appearanceTime: Long,
    school: Int
) extends PassiveBuff {

  def actionTransformer(gameAction: GameAction): Vector[GameAction] = Vector(gameAction)

  def duration: Long = SchoolLockout.duration

  def resourceIdentifier: ResourceIdentifier = school match {
    case MagicSchool.fire => Buff.spiritOfNaraFireLockout
    case MagicSchool.frost => Buff.spiritOfNaraFrostLockout
    case _ => Buff.spiritOfNaraArcaneLockout
  }

  def maybeSourceId: Option[Entity.Id] = None

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty
}

object SchoolLockout {
  inline def duration: Long = 6000L

  /** Whether the entity is currently locked out of the given school. */
  def isLocked(gameState: GameState, entityId: Entity.Id, school: Int): Boolean =
    gameState.allBuffsOfEntity(entityId).exists {
      case lockout: SchoolLockout => lockout.school == school
      case _ => false
    }
}
