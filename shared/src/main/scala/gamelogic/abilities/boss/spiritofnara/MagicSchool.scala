package gamelogic.abilities.boss.spiritofnara

import gamelogic.abilities.WithTargetAbility
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.spiritofnara.PutSchoolLockout
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import services.i18n.I18n

/** The three schools of the spells of the Spirit of Nara. A plain `Int`, so that it travels easily
  * in actions.
  */
object MagicSchool {
  final val fire   = 0
  final val frost  = 1
  final val arcane = 2

  val all: Vector[Int] = Vector(fire, frost, arcane)

  def name(school: Int): String = school match {
    case `fire` => "Fire"
    case `frost` => "Frost"
    case _ => "Arcane"
  }
}

/** One of the main spells of the Spirit of Nara, thrown at a random player.
  *
  * They can be cut, and then the Spirit of Nara can't cast spells of the same [[MagicSchool]] for a
  * while (see [[gamelogic.buffs.boss.spiritofnara.SchoolLockout]]), instead of being silenced.
  */
trait NaraNuke extends WithTargetAbility {

  def school: Int

  def range: Distance = Double.MaxValue

  override def canBeCut: Boolean = true

  override def cutPenalty(time: Long, cutterId: Entity.Id)(using
      IdGeneratorContainer
  ): Vector[GameAction] =
    Vector(PutSchoolLockout(genActionId(), time, genBuffId(), casterId, school))

  def canBeCast(gameState: GameState, time: Long)(using I18n): None.type = None
}
