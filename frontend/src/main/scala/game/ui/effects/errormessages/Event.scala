package game.ui.effects.errormessages

private[errormessages] sealed trait Event {
  def act(state: ErrorMessageState): ErrorMessageState
}

object Event {

  case class MessageRemoved(messageInfo: MessageInfo) extends Event {
    def act(state: ErrorMessageState): ErrorMessageState =
      ErrorMessageState(state.messages.filterNot(_ == messageInfo))
  }
  case class NewMessage(message: String, time: Long) extends Event {
    def act(state: ErrorMessageState): ErrorMessageState =
      ErrorMessageState((MessageInfo(message, time) +: state.messages).take(3))
  }

}
