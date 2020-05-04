package gamelogic.gamestate.statetransformers

import gamelogic.gamestate.GameState
import gamelogic.gamestate.statetransformers.EdgeGameTransformer.EdgeType

final class EdgeGameTransformer(time: Long, edgeType: EdgeType) extends GameStateTransformer {
  def apply(v1: GameState): GameState = edgeType match {
    case EdgeType.Beginning => v1.copy(time = time, startTime = Some(time))
    case EdgeType.Ending    => v1.copy(time = time, endTime   = Some(time))
  }
}

object EdgeGameTransformer {

  sealed trait EdgeType
  object EdgeType {
    case object Beginning extends EdgeType
    case object Ending extends EdgeType
  }

}
