package communication.udp

opaque type MessageKind <: Byte = Byte

object MessageKind {
  val HelloKind: MessageKind    = 1
  val AcceptedKind: MessageKind = 2
  val RejectedKind: MessageKind = 3
  val DataKind: MessageKind     = 4

  val ServerRegisterKind: MessageKind = 16
  val PlayersKind: MessageKind        = 17
  val PlayerRegisterKind: MessageKind = 18
  val RegisteredKind: MessageKind     = 19
  val PunchKind: MessageKind          = 20
}
