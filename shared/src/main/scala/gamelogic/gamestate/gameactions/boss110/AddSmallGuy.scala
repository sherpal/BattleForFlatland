package gamelogic.gamestate.gameactions.boss110

import gamelogic.physics.Complex
import gamelogic.entities.boss.boss110.SmallGuy
import gamelogic.gamestate.GameAction
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.gamestate.statetransformers.WithEntity
import gamelogic.gamestate.statetransformers.GameStateTransformer
import gamelogic.gamestate.GameState

/** Adds an instance of a [[SmallGuy]] in the game.
  */
final case class AddSmallGuy(id: GameAction.Id, time: Long, entityId: Entity.Id, position: Complex)
    extends GameAction
    with GameAction.EntityCreatorAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    new WithEntity(
      SmallGuy(
        entityId,
        time,
        position,
        Angle.zero,
        Angle.zero,
        SmallGuy.fullSpeed,
        moving = false,
        SmallGuy.maxLife,
        Map.empty,
        entityId,
        Map.empty
      ),
      time
    )

  def isLegal(gameState: GameState): Option[String] = Option.empty

  def changeId(newId: GameAction.Id): GameAction = copy(id = newId)

}
