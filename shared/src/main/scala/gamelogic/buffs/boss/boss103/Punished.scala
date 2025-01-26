package gamelogic.buffs.boss.boss103

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions._
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer

/** Puts a buff that prevents its bearer to move or use abilities. If the bearer takes damage, the
  * buff is removed.
  * @param buffId
  *   unique id of the buff
  * @param bearerId
  *   entity id of the bearer
  * @param startingPos
  *   position where the bearer was at start of buff
  * @param appearanceTime
  *   time at which the buff appeared
  */
final case class Punished(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    startingPos: Complex,
    appearanceTime: Long
) extends PassiveBuff {
  def actionTransformer(gameAction: GameAction): Vector[GameAction] = gameAction match {
    case action: MovingBodyMoves if action.entityId == bearerId =>
      Vector(action.copy(moving = false, position = startingPos))
    case action: EntityStartsCasting if action.ability.casterId == bearerId =>
      // prevent user from using abilities
      Vector[GameAction]()
    case action: UseAbility if action.casterId == bearerId =>
      // prevent user from using abilities
      Vector[GameAction]()
    case action: EntityTakesDamage if action.entityId == bearerId =>
      // The bearer took damage, we remove this buff
      Vector(action, RemoveBuff(action.id, action.time, bearerId, buffId))
    case action => Vector(action)
  }

  def duration: Long = Punished.duration

  def resourceIdentifier: ResourceIdentifier = Buff.boss103Punished

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty
}

object Punished {

  val duration: Long = 20000L

}
