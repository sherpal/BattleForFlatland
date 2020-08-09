package gamelogic.buffs.boss.boss103

import gamelogic.buffs.Buff.{Id, ResourceIdentifier}
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.entities.Entity.Id
import gamelogic.gamestate.gameactions.{EntityStartsCasting, EntityTakesDamage, MovingBodyMoves, RemoveBuff, UseAbility}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer

/**
  * Puts a buff that prevents its bearer to move or use abilities. If the bearer takes damage, the buff is removed.
  * @param buffId unique id of the buff
  * @param bearerId entity id of the bearer
  * @param startingPos position where the bearer was at start of buff
  * @param appearanceTime time at which the buff appeared
  */
final case class Punished(buffId: Buff.Id, bearerId: Entity.Id, startingPos: Complex, appearanceTime: Long)
    extends PassiveBuff {
  def actionTransformer(gameAction: GameAction): List[GameAction] = gameAction match {
    case action: MovingBodyMoves if action.entityId == bearerId =>
      List(action.copy(moving = false, position = startingPos))
    case action: EntityStartsCasting if action.ability.casterId == bearerId =>
      // prevent user from using abilities
      List[GameAction]()
    case action: UseAbility if action.casterId == bearerId =>
      // prevent user from using abilities
      List[GameAction]()
    case action: EntityTakesDamage if action.entityId == bearerId =>
      // The bearer took damage, we remove this buff
      List(action, RemoveBuff(action.id, action.time, bearerId, buffId))
    case action => List(action)
  }

  def duration: Long = Punished.duration

  def resourceIdentifier: ResourceIdentifier = Buff.boss103Punished

  def initialActions(gameState: GameState, time: Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] =
    gameState
      .movingBodyEntityById(bearerId)
      .map { movingBody =>
        MovingBodyMoves(
          idGeneratorContainer.gameActionIdGenerator(),
          time,
          bearerId,
          movingBody.currentPosition(time),
          movingBody.direction,
          movingBody.rotation,
          movingBody.speed,
          moving = false
        )
      }
      .toList

  def endingAction(gameState: GameState, time: Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] = Nil
}

object Punished {

  val duration: Long = 20000L

}
