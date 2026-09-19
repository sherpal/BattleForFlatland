package gamelogic.buffs.boss.boss104

import gamelogic.buffs.TickerBuff
import gamelogic.buffs.Buff
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState
import gamelogic.utils.IdGeneratorContainer
import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.gamestate.gameactions.EntityTakesDamage
import utils.misc.RGBColour
import gamelogic.entities.boss.boss104.DebuffCircle
import gamelogic.gamestate.gameactions.RemoveEntity

/** Ticking curse put by [[gamelogic.abilities.boss.boss104.TwinDebuffs]].
  *
  * It damages its bearer every second until someone dispels it. The whole mechanic lives in
  * `endingAction`: dispelling is only safe from inside the [[DebuffCircle]] of the same colour.
  */
final case class TwinDebuff(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    bossId: Entity.Id,
    appearanceTime: Long,
    lastTickTime: Long,
    colour: RGBColour
) extends TickerBuff {

  override def changeLastTickTime(time: Long): TickerBuff = copy(lastTickTime = time)

  override def canBeDispelled: Boolean = true

  override def resourceIdentifier: ResourceIdentifier = Buff.boss104TwinDebuff

  override val tickRate: Long = TwinDebuff.tickRate

  override def duration: Long = TwinDebuff.duration

  /** `maybeDispelledBy` is `Some(playerId)` when a player actively removed the buff, and
    * [[scala.None]] when it simply expired. Here, the dispeller is punished for 90 damage if they
    * were not standing in a circle of the debuff's colour. Either way the circles are cleaned up.
    */
  override def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(
      using IdGeneratorContainer
  ): Vector[GameAction] =
    val debuffCircles = gameState.allTEntities[DebuffCircle].values.filter(_.colour == colour)
    (for {
      dispelledById <- maybeDispelledBy
      dispalledBy   <- gameState.players.get(dispelledById)
      if !debuffCircles.exists(circle => dispalledBy.collides(circle, time))
    } yield EntityTakesDamage(genActionId(), time, dispelledById, 90.0, bossId)).toVector ++
      debuffCircles.map(circle => RemoveEntity(genActionId(), time, circle.id))

  override def tickEffect(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector(
    EntityTakesDamage(genActionId(), time, bearerId, TwinDebuff.damageOnTick, bossId)
  )

}

object TwinDebuff {
  inline def tickRate: Long       = 1000L
  inline def damageOnTick: Double = 10.0
  inline def duration: Long       = 60000L
}
