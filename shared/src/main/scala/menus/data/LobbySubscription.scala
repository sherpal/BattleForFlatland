package menus.data

import io.circe.Codec

/** A subscription to the messages of the menu server (see `communication.udp.Lobby`): for the list
  * of the games (`gameId` empty), or for a game's lobby, which then must stay subscribed (also
  * during the game): the menu server kicks the players who are not.
  *
  * @param id
  *   what the client says in its hellos, and asks the messages with
  * @param key
  *   with which the hellos and the pokes are signed
  * @param udpPort
  *   where the client says hello to (on the menu server's host)
  */
final case class LobbySubscription(id: String, key: String, udpPort: Int)

object LobbySubscription {
  given Codec[LobbySubscription] = io.circe.generic.semiauto.deriveCodec
}

final case class LobbySubscribeFormData(gameId: Option[String])

object LobbySubscribeFormData {
  given Codec[LobbySubscribeFormData] = io.circe.generic.semiauto.deriveCodec
}

final case class LobbyUnsubscribeFormData(id: String)

object LobbyUnsubscribeFormData {
  given Codec[LobbyUnsubscribeFormData] = io.circe.generic.semiauto.deriveCodec
}

/** A message of a subscription, with its number (they start at 1, and increase by one). */
final case class LobbyMessage(number: Long, message: MenuGameComm)

object LobbyMessage {
  given Codec[LobbyMessage] = io.circe.generic.semiauto.deriveCodec
}
