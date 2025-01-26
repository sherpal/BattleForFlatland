package gamelogic.buffs.abilities.classes

import gamelogic.abilities.triangle.DirectHit
import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Multiply all damages done by the [[gamelogic.abilities.triangle.DirectHit]] by multiplying its
  * damage by `UpgradeDirectHit.damageIncrease`. This buff is cumulative.
  */
final case class UpgradeDirectHit(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long
) extends PassiveBuff {

  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty

  def actionTransformer(gameAction: GameAction): Vector[GameAction] = gameAction match {
    case action @ EntityStartsCasting(_, _, _, ability) if ability.casterId == bearerId =>
      Vector(action.copy(ability = ability match {
        case ability: DirectHit =>
          ability.copy(damage = (ability.damage * UpgradeDirectHit.damageIncrease).toInt)
        case _ => ability
      }))

    case _ => Vector(gameAction)
  }

  def duration: Long = UpgradeDirectHit.duration

  def resourceIdentifier: ResourceIdentifier = Buff.triangleUpgradeDirectHit
}

object UpgradeDirectHit {

  def duration: Long = 20000L

  def damageIncrease: Double = 1.2

}
