package game.scenes.loading

sealed trait LoadingState

object LoadingState {
  case class NotStarted()                        extends LoadingState
  case class InProgress(percentage: Int)         extends LoadingState
  case class WaitingForOthers()                  extends LoadingState
  case class Error(key: String, message: String) extends LoadingState
}
