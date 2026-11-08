package gamelogic.gamestate.gameactions.spiritofnara

import gamelogic.buffs.Buff
import gamelogic.buffs.boss.spiritofnara.SchoolLockout
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}
import gamelogic.gamestate.{GameAction, GameState}
import services.i18n.I18n

/** Locks the Spirit of Nara out of a [[gamelogic.abilities.boss.spiritofnara.MagicSchool]] (see
  * [[SchoolLockout]]).
  */
final case class PutSchoolLockout(
    id: GameAction.Id,
    time: Long,
    buffId: Buff.Id,
    bearerId: Entity.Id,
    school: Int
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    WithBuff(SchoolLockout(buffId, bearerId, time, school))

  def isLegal(gameState: GameState)(using I18n): None.type = None

  def changeId(newId: Id): PutSchoolLockout = copy(id = newId)
}
