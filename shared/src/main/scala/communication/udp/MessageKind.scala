package communication.udp

opaque type MessageKind <: Byte = Byte

object MessageKind {
  val HelloKind: MessageKind     = 1
  val AcceptedKind: MessageKind  = 2
  val RejectedKind: MessageKind  = 3
  val DataKind: MessageKind      = 4
  val ChallengeKind: MessageKind = 5
  val ProofKind: MessageKind     = 6

  val ServerRegisterKind: MessageKind = 16
  val PlayersKind: MessageKind        = 17
  val PlayerRegisterKind: MessageKind = 18
  val RegisteredKind: MessageKind     = 19
  val PunchKind: MessageKind          = 20

  val LobbyHelloKind: MessageKind = 32
  val LobbyPokeKind: MessageKind  = 33
}
