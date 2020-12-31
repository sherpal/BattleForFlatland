package gamelogic.buffs.abilities.classes

import gamelogic.abilities.triangle.DirectHit
import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/**
  * Multiply all damages done by the [[gamelogic.abilities.triangle.DirectHit]] by multiplying its damage by
  * `UpgradeDirectHit.damageIncrease`.
  * This buff is cumulative.
  */
final case class UpgradeDirectHit(buffId: Buff.Id, bearerId: Entity.Id, appearanceTime: Long) extends PassiveBuff {

  def endingAction(gameState: GameState, time: Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] = Nil

  def actionTransformer(gameAction: GameAction): List[GameAction] = gameAction match {
    case action @ EntityStartsCasting(_, _, _, ability) if ability.casterId == bearerId =>
      action.copy(ability = ability match {
        case ability: DirectHit => ability.copy(damage = (ability.damage * UpgradeDirectHit.damageIncrease).toInt)
        case _                  => ability
      }) :: Nil

    case _ => gameAction :: Nil
  }

  def duration: Long = UpgradeDirectHit.duration

  def resourceIdentifier: ResourceIdentifier = Buff.triangleUpgradeDirectHit
}

object UpgradeDirectHit {

  def duration: Long = 20000L

  def damageIncrease: Double = 1.2

}
