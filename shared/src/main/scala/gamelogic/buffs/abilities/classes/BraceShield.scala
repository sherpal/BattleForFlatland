package gamelogic.buffs.abilities.classes

import gamelogic.buffs.{Buff, PassiveBuff, StackedBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.physics.PosInt

/** The shield of the square (see [[gamelogic.abilities.square.Brace]]). Each stack reduces all
  * damage taken by the bearer by multiplying it by `BraceShield.damageMultiplier`. After
  * `BraceShield.duration`, the shield loses a stack (see [[gamelogic.buffs.StackedBuff]]).
  */
final case class BraceShield(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long,
    stacks: PosInt
) extends PassiveBuff
    with StackedBuff {

  def actionTransformer(gameAction: GameAction): Vector[GameAction] =
    Vector(gameAction match {
      case gameAction: EntityTakesDamage if gameAction.entityId == bearerId =>
        gameAction.copy(amount =
          gameAction.amount * math.pow(BraceShield.damageMultiplier, stacks.toInt)
        )
      case _ => gameAction
    })

  def duration: Long = BraceShield.duration

  def resourceIdentifier: Buff.StackedBuffIdentifier = Buff.squareBrace
}

object BraceShield {

  /** Multiplier applied to all damage taken by the bearer, for each stack. */
  inline def damageMultiplier: Double = 0.95

  /** Time (in millis) after which the shield loses a stack. */
  inline def duration: Long = 4000L

  /** Maximum number of stacks. */
  inline def maxStacks: PosInt = PosInt.of(5)

  /** The shields that `entityId` currently has. There should be at most one. */
  def on(gameState: GameState, entityId: Entity.Id): Vector[BraceShield] =
    gameState.allBuffsOfEntity(entityId).collect { case shield: BraceShield => shield }.toVector

}
