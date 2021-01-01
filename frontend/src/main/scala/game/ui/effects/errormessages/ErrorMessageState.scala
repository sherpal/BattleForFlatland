package game.ui.effects.errormessages

private[errormessages] final case class ErrorMessageState(
    messages: List[MessageInfo]
) {

  def messageStatesNow(time: Long): List[(MessageInfo, Double)] =
    messages.map(mi => (mi, mi.alphaValue(time)))

}

object ErrorMessageState {
  def empty: ErrorMessageState = ErrorMessageState(Nil)
}
