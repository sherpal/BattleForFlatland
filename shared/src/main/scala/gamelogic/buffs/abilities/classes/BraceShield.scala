package gamelogic.buffs.abilities.classes

import gamelogic.buffs.{Buff, PassiveBuff, StackedBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.gameactions.{EntityTakesDamage, PutStackedBuff, RemoveBuff}
import gamelogic.physics.PosInt
import gamelogic.utils.{IdGeneratorContainer, IdsProducer}

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

  /** The number of stacks that `entityId` currently has, if any. */
  def stacksOn(gameState: GameState, entityId: Entity.Id): Option[PosInt] =
    // Normally at most one shield, but if two ever coexist, the biggest one wins.
    on(gameState, entityId).map(_.stacks).maxOption

  /** Actions replacing the shield of `entityId` (if any) by one with `stacks` stacks (if any),
    * starting at `time`. The number of stacks can't go above `maxStacks`.
    */
  def setStacks(gameState: GameState, entityId: Entity.Id, time: Long, stacks: Option[PosInt])(using
      IdGeneratorContainer
  ): Vector[GameAction] =
    on(gameState, entityId).map(shield =>
      RemoveBuff(IdsProducer.genActionId(), time, entityId, shield.buffId)
    ) ++ stacks.map(newStacks =>
      PutStackedBuff(
        IdsProducer.genActionId(),
        time,
        IdsProducer.genBuffId(),
        entityId,
        entityId,
        time,
        Buff.squareBrace,
        newStacks.min(maxStacks)
      )
    )

}
