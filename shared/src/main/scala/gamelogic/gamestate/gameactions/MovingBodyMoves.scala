package gamelogic.gamestate.gameactions

import gamelogic.buffs.MovementSpeedModifier
import gamelogic.entities.Entity
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.BossEntity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.entities.boss.boss110.CreepingShadow
import services.i18n.I18n

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
        // the speed of a player is decided by its buffs (slows), not by the player
        val actualSpeed = movingBody match {
          case player: PlayerClass => MovementSpeedModifier.playerSpeed(gameState, player)
          case _ => speed
        }
        new WithEntity(
          movingBody.move(time, position, direction, rotation, actualSpeed, moving),
          time
        )
      }

  def isLegal(gameState: GameState)(using i18n: I18n): Option[String] =
    gameState.movingBodyEntityById(entityId) match {
      case None => Some(i18n.entityIsNotMoving(entityId))
      case Some(_: BossEntity) => None
      case Some(_: CreepingShadow) => None
      case Some(movingBody) =>
        val afterMovement = movingBody.move(time, position, direction, rotation, speed, moving)
        Option.unless(
          gameState.obstaclesLike.forall(obstacle => !obstacle.collides(afterMovement, time))
        )(
          i18n.entityWouldCollide(entityId)
        )
    }

  def changeId(newId: Id): GameAction = copy(id = newId)
}
