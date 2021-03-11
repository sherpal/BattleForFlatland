package gamelogic.gamestate.gameactions

import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.BossEntity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.entities.boss.boss110.CreepingShadow

final case class MovingBodyMoves(
    id: GameAction.Id,
    time: Long,
    entityId: Entity.Id,
    position: Complex,
    direction: Angle,
    rotation: Angle,
    speed: Double,
    moving: Boolean
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState
      .movingBodyEntityById(entityId)
      .fold(GameStateTransformer.identityTransformer) { movingBody =>
        new WithEntity(movingBody.move(time, position, direction, rotation, speed, moving), time)
      }

  def isLegal(gameState: GameState): Option[String] = gameState.movingBodyEntityById(entityId) match {
    case None                    => Some(s"Entity $entityId does not exist, or it is not a moving body")
    case Some(_: BossEntity)     => None
    case Some(_: CreepingShadow) => None
    case Some(movingBody) =>
      val afterMovement = movingBody.move(time, position, direction, rotation, speed, moving)
      Option.unless(gameState.obstaclesLike.forall(obstacle => !obstacle.collides(afterMovement, time)))(
        s"Entity $entityId will collide an obstacle if it proceed"
      )
  }

  def changeId(newId: Id): GameAction = copy(id = newId)
}
