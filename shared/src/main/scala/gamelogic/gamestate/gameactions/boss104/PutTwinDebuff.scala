package gamelogic.gamestate.gameactions.boss104

import gamelogic.gamestate.GameAction
import gamelogic.buffs.Buff
import gamelogic.entities.Entity
import utils.misc.RGBColour
import gamelogic.physics.Complex
import gamelogic.gamestate.GameState
import gamelogic.gamestate.statetransformers.GameStateTransformer
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.WithBuff
import gamelogic.gamestate.statetransformers.WithEntity
import gamelogic.buffs.boss.boss104.TwinDebuff
import gamelogic.entities.boss.boss104.DebuffCircle

/** Materialises one half of the [[gamelogic.abilities.boss.boss104.TwinDebuffs]] ability: the curse
  * on one player, plus the coloured circle on the ground that goes with it.
  *
  * All the randomness of the ability has already been resolved by the time this action exists:
  * the bearer, the colour, the circle's id and its position are all constructor arguments. This is
  * what makes `createGameStateTransformer` below pure, and hence replayable identically on every
  * client.
  */
final case class PutTwinDebuff(
    id: GameAction.Id,
    time: Long,
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    colour: RGBColour,
    circleId: Entity.Id,
    circlePosition: Complex
) extends GameAction {

  override def changeId(newId: Id): GameAction = copy(id = newId)

  override def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    WithBuff(TwinDebuff(buffId, bearerId, sourceId, time, time, colour)) ++ WithEntity(
      DebuffCircle(circleId, time, circlePosition, colour),
      time
    )

  override def isLegal(gameState: GameState): Option[String] = None

}
