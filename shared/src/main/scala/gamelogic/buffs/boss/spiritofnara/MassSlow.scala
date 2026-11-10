package gamelogic.buffs.boss.spiritofnara

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, MovementSpeedModifier, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.RefreshMovementSpeed
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Slows the bearer down, while it runs away from the Arcane Explosion. */
final case class MassSlow(buffId: Buff.Id, bearerId: Entity.Id, appearanceTime: Long)
    extends PassiveBuff
    with MovementSpeedModifier {

  def speedMultiplier: Double = MassSlow.speedMultiplier

  def actionTransformer(gameAction: GameAction): Vector[GameAction] = Vector(gameAction)

  def duration: Long = MassSlow.duration

  def resourceIdentifier: ResourceIdentifier = Buff.spiritOfNaraMassSlow

  def maybeSourceId: Option[Entity.Id] = None

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector(RefreshMovementSpeed(genActionId(), time, bearerId))
}

object MassSlow {
  inline def duration: Long          = 8000L
  inline def speedMultiplier: Double = 0.5
}
