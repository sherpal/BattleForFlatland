package gamelogic.gamestate.gameactions

import gamelogic.entities.{Entity, SimpleBulletBody}
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithSimpleBullet}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

/** Adds a bullet to the game. */
final case class NewSimpleBullet(
    id: GameAction.Id,
    time: Long,
    bulletId: Entity.Id,
    pos: Complex,
    speed: Double,
    direction: Double,
    ownerId: Entity.Id,
    range: Double
) extends GameAction {

  def isLegal(gameState: GameState): None.type = None

  def changeId(newId: Id): GameAction = copy(id = newId)

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    new WithSimpleBullet(
      SimpleBulletBody(bulletId, time, pos, speed, SimpleBulletBody.defaultRadius, direction, range, ownerId)
    )
}
