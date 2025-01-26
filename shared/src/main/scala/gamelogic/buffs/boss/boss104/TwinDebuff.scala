package gamelogic.buffs.boss.boss104

import gamelogic.buffs.TickerBuff
import gamelogic.buffs.Buff
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState
import gamelogic.utils.IdGeneratorContainer
import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.gamestate.gameactions.EntityTakesDamage
import utils.misc.RGBAColour
import gamelogic.entities.boss.boss104.DebuffCircle

final case class TwinDebuff(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    bossId: Entity.Id,
    appearanceTime: Long,
    lastTickTime: Long,
    colour: RGBAColour
) extends TickerBuff {

  override def changeLastTickTime(time: Long): TickerBuff = copy(lastTickTime = time)

  override def resourceIdentifier: ResourceIdentifier = Buff.boss104TwinDebuff

  override val tickRate: Long = TwinDebuff.tickRate

  override def duration: Long = TwinDebuff.duration

  override def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(
      using IdGeneratorContainer
  ): Vector[GameAction] = (for {
    dispelledById <- maybeDispelledBy
    dispalledBy   <- gameState.players.get(dispelledById)
    debuffCircles = gameState.allTEntities[DebuffCircle].values
    if !debuffCircles.exists(circle =>
      circle.colour == colour && dispalledBy.collides(circle, time)
    )
  } yield EntityTakesDamage(genActionId(), time, dispelledById, 90.0, bossId)).toVector

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
