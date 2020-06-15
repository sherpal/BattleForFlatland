package gamelogic.gamestate.gameactions

import gamelogic.entities.Entity
import gamelogic.entities.Entity.TeamId
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.movingstuff.PentagonBullet
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

final case class NewPentagonBullet(
    id: GameAction.Id,
    time: Long,
    bulletId: Entity.Id,
    pos: Complex,
    speed: Double,
    direction: Angle,
    range: Double,
    damage: Double,
    ownerId: Entity.Id,
    teamId: TeamId,
    colour: Int
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer = new WithEntity(
    PentagonBullet(bulletId, time, pos, speed, direction, range, damage, ownerId, teamId, colour),
    time
  )

  def isLegal(gameState: GameState): Boolean = true

  def changeId(newId: Id): GameAction = copy(id = newId)
}
