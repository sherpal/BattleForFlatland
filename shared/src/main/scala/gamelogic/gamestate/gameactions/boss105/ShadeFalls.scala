package gamelogic.gamestate.gameactions.boss105

import gamelogic.buffs.Buff
import gamelogic.buffs.boss.boss105.Mourning
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss105.ReformationPoint
import gamelogic.gamestate.GameAction.{EntityCreatorAction, Id}
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

/** What happens to the boss when its Shade falls: it starts [[Mourning]], and the place where the
  * Shade will come back is marked on the ground.
  */
final case class ShadeFalls(
    id: GameAction.Id,
    time: Long,
    bossId: Entity.Id,
    mourningBuffId: Buff.Id,
    entityId: Entity.Id,
    position: Complex
) extends GameAction
    with EntityCreatorAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    WithEntity(ReformationPoint(entityId, time, position), time) ++
      WithBuff(Mourning(mourningBuffId, bossId, time, entityId, position))

  def isLegal(gameState: GameState): None.type = None

  def changeId(newId: Id): ShadeFalls = copy(id = newId)
}
