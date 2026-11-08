package gamelogic.buffs.boss.spiritofnara

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, MovementSpeedModifier, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.RefreshMovementSpeed
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Slows the bearer down. Put by the Frostbolt and the Blizzard of the Spirit of Nara. */
final case class Chilled(buffId: Buff.Id, bearerId: Entity.Id, appearanceTime: Long)
    extends PassiveBuff
    with MovementSpeedModifier {

  def speedMultiplier: Double = Chilled.speedMultiplier

  def actionTransformer(gameAction: GameAction): Vector[GameAction] = Vector(gameAction)

  def duration: Long = Chilled.duration

  def resourceIdentifier: ResourceIdentifier = Buff.spiritOfNaraChilled

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector(RefreshMovementSpeed(genActionId(), time, bearerId))
}

object Chilled {
  inline def duration: Long          = 4000L
  inline def speedMultiplier: Double = 0.5
}
