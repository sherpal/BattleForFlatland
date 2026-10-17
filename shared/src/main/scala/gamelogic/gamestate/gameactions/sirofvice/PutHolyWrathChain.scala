package gamelogic.gamestate.gameactions.sirofvice

import gamelogic.entities.Entity
import gamelogic.entities.boss.sirofvice.HolyWrathChain
import gamelogic.gamestate.statetransformers.{
  GameStateTransformer,
  RemoveEntityTransformer,
  WithEntity
}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

/** Replaces the previous [[HolyWrathChain]] (if any) by a new one, going through `positions`. */
final case class PutHolyWrathChain(
    id: GameAction.Id,
    time: Long,
    entityId: Entity.Id,
    positions: Vector[Complex]
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState
      .allTEntities[HolyWrathChain]
      .keys
      .toVector
      .sortBy(_.value)
      .map(chainId => RemoveEntityTransformer(chainId, time): GameStateTransformer)
      .foldLeft(GameStateTransformer.identityTransformer)(_ ++ _) ++
      WithEntity(HolyWrathChain(entityId, time, positions), time)

  def isLegal(gameState: GameState): None.type = None

  def changeId(newId: GameAction.Id): PutHolyWrathChain = copy(id = newId)
}
