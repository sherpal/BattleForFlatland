package gamelogic.gamestate

import gamelogic.entities.{DummyLivingEntity, Entity}

/**
  * A [[gamelogic.gamestate.GameState]] has the complete knowledge of everything that exists in the game.
  * Having an instance of a GameState is enough to have all information about the game at that particular moment in time.
  *
  * @param time in millis
  */
final case class GameState(time: Long, startTime: Option[Long], players: Map[Entity.Id, DummyLivingEntity]) {

  def started: Boolean = startTime.isDefined

  /**
    * Applies the effects of all the actions in the list to this [[GameState]].
    * Actions are assumed to be ordered in time already.
    */
  def applyActions(actions: List[GameAction]): GameState = actions.foldLeft(this) { (currentGameState, nextAction) =>
    nextAction(currentGameState)
  }

  /** Modifies the given player at the given time. */
  def withPlayer(newTime: Long, player: DummyLivingEntity): GameState =
    copy(time = newTime, players = players + (player.id -> player))

  /** Modifies the timestamp of the game. */
  def timeUpdate(newTime: Long): GameState = copy(time = newTime)

}
