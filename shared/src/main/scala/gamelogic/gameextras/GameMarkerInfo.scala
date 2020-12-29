package gamelogic.gameextras

import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import gamelogic.entities.Entity

/**
  * A [[GameMarkerInfo]] remembers all there is to know for a certain [[GameMarker]].
  *
  * The most important things are what marker is represented, and where it should be
  * at a certain time.
  */
sealed trait GameMarkerInfo {
  def marker: GameMarker

  /**
    * Returns the game position of the [[GameMarker]] at the specified time and for
    * the specified [[GameState]].
    */
  def maybePosition(gameState: GameState, currentTime: Long): Option[Complex]
}

object GameMarkerInfo {

  /**
    * Fixed marker which stays on the ground, and never moves.
    */
  case class FixedGameMarker(marker: GameMarker, gamePosition: Complex) extends GameMarkerInfo {
    def maybePosition(gameState: GameState, currentTime: Long): Option[Complex] = Some(gamePosition)
  }

  /**
    * [[GameMarker]] attached to a certain [[Entity]] in the game.
    * In practice, this entity should always be a [[gamelogic.entities.Body]], since
    * we at least need to have a position.
    */
  case class GameMarkerOnEntity(marker: GameMarker, entityId: Entity.Id) extends GameMarkerInfo {
    def maybePosition(gameState: GameState, currentTime: Long): Option[Complex] =
      gameState.movingBodyEntityById(entityId).map(_.currentPosition(currentTime))
  }

}
