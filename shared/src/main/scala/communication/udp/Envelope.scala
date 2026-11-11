package communication.udp

import boopickle.Default.*

import java.nio.ByteBuffer
import scala.util.Try

/** What travels in a datagram between a player and the game server, around the packets of a
  * [[Connection]]: the first byte tells what the datagram is.
  *
  * A [[Connection]] knows nothing about who is on the other side. The server therefore only creates
  * one once a datagram from a new address has proven that it comes from a legit player, which takes
  * a challenge, so that the secret of the player never travels (the datagrams are not encrypted,
  * and anyone on the way sees them):
  *
  *   - the player sends [[Packet.Hello]] (their name, the game, a nonce of theirs);
  *   - the server answers [[Packet.Challenge]], with a nonce of its own, made for that address (see
  *     [[Challenges]], it remembers nothing until the player passed);
  *   - the player answers [[Packet.Proof]]: the tag of both nonces with their secret (see
  *     [[Hmac]]);
  *   - the server answers [[Packet.Accepted]] (with a tag that only who knows the secret can make:
  *     it is the game server), or [[Packet.Rejected]].
  *
  * Both then derive the keys of the session from the secret and the nonces ([[SessionKeys]]), and
  * exchange [[Packet.Data]], which contain the packets of the [[Connection]], numbered and tagged:
  * the ones that do not come from the other side, or that come again, are dropped.
  *
  * Hello and Proof are only decoded by the game server, on the JVM (where decoding garbage fails
  * cleanly); what the players decode has a fixed layout (the client runs in Lua, where reading past
  * the end of an array is not checked).
  *
  * The datagrams being possibly lost, the client must keep sending Hello until challenged, then
  * Proof until accepted; the server answers every one of them, so they are idempotent. A server
  * receiving Data from an address it does not know (e.g., after a restart, or once the connection
  * was dropped) answers `Rejected(RejectReason.NoSession)`.
  */
object Envelope {

  sealed trait Packet
  object Packet {
    case class Hello(userName: String, gameId: Int, clientNonce: Nonce) extends Packet
    case class Challenge(clientNonce: Nonce, serverNonce: Nonce) extends Packet
    case class Proof(userName: String, gameId: Int, nonces: Challenge, tag: Tag) extends Packet

    /** `tag`: of [[AcceptedLabel]], with the key of the session towards the player. */
    case class Accepted(tag: Tag) extends Packet
    case class Rejected(reason: Byte) extends Packet

    /** `number`: strictly increasing on each side of a session; `tag`: of the number and the
      * payload, with the key of the session in that direction.
      */
    case class Data(number: Long, payload: Array[Byte], tag: Tag) extends Packet
  }

  /** 16 random bytes, or a tag (see [[Hmac.TagSize]]). Wrapped, since arrays have no equality. */
  final case class Nonce(bytes: Vector[Byte]) {
    def toArray: Array[Byte] = bytes.toArray
  }
  type Tag = Nonce

  object Nonce {
    val Size = 16

    def apply(bytes: Array[Byte]): Nonce = new Nonce(bytes.toVector)

    /** Made of the time and of `random`: different from all the others, which is all we need. */
    def fresh(random: () => Int): Nonce =
      Nonce(Hmac.longBytes(System.currentTimeMillis()) ++ Array.fill[Byte](8)(random().toByte))
  }

  object RejectReason {

    /** The server is not ready to accept players yet. */
    val NotReady: Byte = 0

    /** Wrong game, unknown player or wrong secret. */
    val Unauthorized: Byte = 1

    /** Data was received from an address that did not say hello. */
    val NoSession: Byte = 2
  }

  /** Hello, Challenge and Proof are much smaller than this: bigger datagrams are not even looked
    * at.
    */
  val MaxHandshakeSize = 512

  private val AcceptedLabel = Hmac.utf8("accepted")

  import MessageKind.*

  private def withKind(kind: MessageKind, bb: ByteBuffer): Array[Byte] = {
    val result = new Array[Byte](bb.remaining() + 1)
    result(0) = kind
    bb.get(result, 1, bb.remaining())
    result
  }

  def encode(packet: Packet): Array[Byte] = packet match {
    case Packet.Hello(userName, gameId, nonce) =>
      withKind(HelloKind, Pickle.intoBytes((userName, gameId, nonce.bytes)))
    case Packet.Challenge(clientNonce, serverNonce) =>
      Hmac.concat(Seq(Array(ChallengeKind), clientNonce.toArray, serverNonce.toArray))
    case Packet.Proof(userName, gameId, nonces, tag) =>
      withKind(
        ProofKind,
        Pickle.intoBytes(
          (userName, gameId, nonces.clientNonce.bytes, nonces.serverNonce.bytes, tag.bytes)
        )
      )
    case Packet.Accepted(tag) => AcceptedKind +: tag.toArray
    case Packet.Rejected(reason) => Array(RejectedKind, reason)
    case Packet.Data(number, payload, tag) =>
      Hmac.concat(Seq(Array(DataKind), Hmac.longBytes(number), payload, tag.toArray))
  }

  /** Decodes a datagram, if it is one. */
  def parse(datagram: Array[Byte]): Option[Packet] =
    if datagram.isEmpty then None
    else {
      def payload = ByteBuffer.wrap(datagram, 1, datagram.length - 1)
      def handshake[A](decode: => A): Option[A] =
        if datagram.length > MaxHandshakeSize then None else Try(decode).toOption
      datagram(0) match {
        case DataKind if datagram.length >= 1 + 8 + Hmac.TagSize =>
          Some(
            Packet.Data(
              Hmac.readLong(datagram, 1),
              datagram.slice(9, datagram.length - Hmac.TagSize),
              Nonce(datagram.takeRight(Hmac.TagSize))
            )
          )
        case AcceptedKind if datagram.length == 1 + Hmac.TagSize =>
          Some(Packet.Accepted(Nonce(datagram.drop(1))))
        case RejectedKind => Option.when(datagram.length == 2)(Packet.Rejected(datagram(1)))
        case HelloKind =>
          handshake {
            val (userName, gameId, nonce) =
              Unpickle[(String, Int, Vector[Byte])].fromBytes(payload)
            Packet.Hello(userName, gameId, Nonce(nonce))
          }
        case ChallengeKind if datagram.length == 1 + 2 * Nonce.Size =>
          Some(
            Packet.Challenge(
              Nonce(datagram.slice(1, 1 + Nonce.Size)),
              Nonce(datagram.drop(1 + Nonce.Size))
            )
          )
        case ProofKind =>
          handshake {
            val (userName, gameId, client, server, tag) =
              Unpickle[(String, Int, Vector[Byte], Vector[Byte], Vector[Byte])]
                .fromBytes(payload)
            Packet.Proof(
              userName,
              gameId,
              Packet.Challenge(Nonce(client), Nonce(server)),
              Nonce(tag)
            )
          }
        case _ => None
      }
    }

  /** What the player proves with: the tag of the nonces (and of who they are) with their secret. */
  def proof(
      userName: String,
      gameId: Int,
      nonces: Packet.Challenge,
      secret: Hmac
  ): Packet.Proof =
    Packet.Proof(userName, gameId, nonces, Nonce(proofTag(userName, gameId, nonces, secret)))

  def isProven(proof: Packet.Proof, secret: Hmac): Boolean =
    Hmac.sameBytes(proofTag(proof.userName, proof.gameId, proof.nonces, secret), proof.tag.toArray)

  private def proofTag(userName: String, gameId: Int, nonces: Packet.Challenge, secret: Hmac) =
    secret.tag(
      Array(ProofKind),
      Hmac.utf8(s"$userName\u0000$gameId\u0000"),
      nonces.clientNonce.toArray,
      nonces.serverNonce.toArray
    )

  /** The keys of a session, on one side (`towardsServer` tells which one): derived from the secret
    * of the player and the nonces of the handshake, so that they are different for every session.
    * Not thread-safe: one per session, used by its connection.
    */
  final class SessionKeys(secret: Hmac, nonces: Packet.Challenge, towardsServer: Boolean) {
    private val session =
      secret.derive("session", nonces.clientNonce.toArray, nonces.serverNonce.toArray)
    private val toServer = session.derive("to the server")
    private val toPlayer = session.derive("to the player")

    private val sending   = if towardsServer then toServer else toPlayer
    private val receiving = if towardsServer then toPlayer else toServer

    private var lastSent = 0L
    private val window   = ReplayWindow()

    /** The datagram of the next payload that we send. */
    def seal(payload: Array[Byte]): Array[Byte] = synchronized {
      lastSent += 1
      encode(Packet.Data(lastSent, payload, Nonce(sending.tag(Hmac.longBytes(lastSent), payload))))
    }

    /** The payload, if the datagram comes from the other side, and not again. */
    def open(data: Packet.Data): Option[Array[Byte]] = synchronized {
      Option.when(
        receiving.verify(data.tag.toArray, Hmac.longBytes(data.number), data.payload) &&
          window.accept(data.number)
      )(data.payload)
    }

    /** The accepted datagram, that the server sends. */
    def accepted: Packet.Accepted = Packet.Accepted(Nonce(toPlayer.tag(AcceptedLabel)))

    /** Whether the accepted datagram comes from the game server (it knows the secret). */
    def isAccepted(accepted: Packet.Accepted): Boolean =
      toPlayer.verify(accepted.tag.toArray, AcceptedLabel)
  }

  /** The numbers already received among the [[ReplayWindow.Size]] latest: datagrams may arrive out
    * of order, but not twice. Older ones are dropped (the [[Connection]] sends them again if they
    * matter).
    */
  final class ReplayWindow {
    private var highest = 0L
    private var seen    = 0L // bit i: highest - i was received

    def accept(number: Long): Boolean =
      if number > highest then {
        val shift = number - highest
        seen = if shift >= ReplayWindow.Size then 1L else (seen << shift) | 1L
        highest = number
        true
      } else {
        val age = highest - number
        if number <= 0 || age >= ReplayWindow.Size || (seen & (1L << age)) != 0 then false
        else {
          seen |= 1L << age
          true
        }
      }
  }

  object ReplayWindow {
    val Size = 64
  }

  /** The nonces that the server challenges the players with: a tag of their address, of their nonce
    * and of the current minute, with a key of its own, so that it remembers nothing about the
    * players that did not prove anything yet, and a proof is only good for the address it was made
    * for (and for a minute or two).
    */
  final class Challenges(key: Hmac, now: () => Long = () => System.currentTimeMillis()) {
    private def nonce(from: Peer, userName: String, clientNonce: Nonce, minute: Long): Nonce =
      Nonce(
        key.tag(
          Hmac.utf8(s"${from.address}\u0000${from.port}\u0000$userName\u0000"),
          clientNonce.toArray,
          Hmac.longBytes(minute)
        )
      )

    private def minute = now() / 60000

    def challenge(hello: Packet.Hello, from: Peer): Packet.Challenge =
      Packet.Challenge(hello.clientNonce, nonce(from, hello.userName, hello.clientNonce, minute))

    /** Whether we challenged that address with these nonces lately. */
    def isOurs(proof: Packet.Proof, from: Peer): Boolean =
      Seq(minute, minute - 1).exists(m =>
        nonce(from, proof.userName, proof.nonces.clientNonce, m) == proof.nonces.serverNonce
      )
  }

  /** Makes a [[UDPSocket]] out of another one that wraps everything it sends in a [[Packet.Data]]
    * of the session. Used by both sides to give to a [[Connection]], after the handshake. The other
    * methods are the ones of the underlying socket.
    */
  final class DataSocket(underlying: UDPSocket, keys: SessionKeys) extends UDPSocket {
    def peer: Peer = underlying.peer

    def bind(port: Int, address: Option[String]): Unit = underlying.bind(port, address)

    def send(message: Array[Byte], port: Int, address: String): Unit =
      underlying.send(keys.seal(message), port, address)

    def close(): Unit = underlying.close()
  }

}
