package gamelogic.buffs.boss.boss103

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/**
  * Deals `appearanceDamage` when the buff is applied, and silence the bearer for 4 seconds.
  * Silence means that the bearer can't use an ability that require them to use mana.
  *
  * This debuff should be avoided by Pentagons and Hexagons during the game, except when the boss uses its Punishment
  * ability.
  */
final case class Purified(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long
) extends PassiveBuff {
  def actionTransformer(gameAction: GameAction): List[GameAction] = gameAction match {
    case EntityStartsCasting(_, _, _, ability) if ability.resource == Resource.Mana && ability.casterId == bearerId =>
      Nil // prevent abilities using mana from being used
    case action => List(action)
  }

  def duration: Long = Purified.duration

  def resourceIdentifier: ResourceIdentifier = Buff.boss103Purified

  def endingAction(gameState: GameState, time: Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] = Nil
}

object Purified {

  final val duration: Long = 4000L

}
