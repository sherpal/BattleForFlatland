package server

import communication.udp.{Hmac, Lobby, Peer, UDPSocket}
import menus.data.{GameServerAddress, LobbyMessage, LobbySubscription, MenuGameComm, User}
import services.events
import zio.*

import java.security.SecureRandom
import java.util.concurrent.{Executors, TimeUnit}

/** The menu server's side of the lobby (see `communication.udp.Lobby`): the subscriptions of the
  * clients, the messages that they did not get yet, and where to poke them when there are new ones.
  *
  * The subscriptions of a game's lobby tell whether the player is still there (they used to keep a
  * websocket open): the first hello dispatches `UserConnectedSocket`, and going silent for
  * [[Lobby.Timeout]] (or unsubscribing) `UserSocketDisconnected` (see
  * `programs.userConnectedWatcher`, which kicks the players who stay away).
  *
  * @param socket
  *   the UDP socket of the menu server (shared with the rendezvous, see [[Lobby.SharedSocket]])
  * @param run
  *   runs an effect of the menu server (and waits for it)
  */
final class LobbyServer(
    socket: UDPSocket,
    udpPort: => Int,
    run: ZIO[BackendEnv, Nothing, Any] => Unit
) {
  import LobbyServer.*

  private final class Subscription(
      val id: String,
      val user: User,
      val gameId: Option[String],
      val key: Hmac,
      val createdAt: Long
  ) {
    var messages: Vector[LobbyMessage] = Vector.empty // not known to be received yet
    var latest: Long                   = 0L
    var fetched: Long                  = 0L // the number of the latest message given out
    var peer: Option[Peer]             = None
    var lastHello: Long                = 0L // its number
    var lastSeen: Long                 = createdAt
    @volatile var closed               = false
  }

  private val lock          = new Object
  private var subscriptions = Map.empty[String, Subscription]
  private val random        = SecureRandom()

  private val sweeper = Executors.newSingleThreadScheduledExecutor { runnable =>
    val thread = Thread(runnable, "lobby-sweeper")
    thread.setDaemon(true)
    thread
  }
  sweeper.scheduleWithFixedDelay(() => sweep(), 5, 5, TimeUnit.SECONDS)

  private def now = java.lang.System.currentTimeMillis()

  private def randomToken(): String = {
    val bytes = new Array[Byte](18)
    random.nextBytes(bytes)
    java.util.Base64.getUrlEncoder.withoutPadding.encodeToString(bytes)
  }

  def subscribe(user: User, gameId: Option[String]): LobbySubscription = {
    val key          = randomToken()
    val subscription = Subscription(randomToken(), user, gameId, Hmac(key), now)
    lock.synchronized(subscriptions += subscription.id -> subscription)
    run(
      events.registerEvents[
        events.Event.GameDataRefreshed | events.Event.GameStarted |
          events.Event.HostCandidacyRequested | events.Event.HostElected |
          events.Event.GameLaunchFailed | events.Event.GameCredentials
      ](ZIO.succeed(!subscription.closed)) { event =>
        ZIO.succeed(push(subscription, messagesFor(event, user, gameId)))
      }
    )
    LobbySubscription(subscription.id, key, udpPort)
  }

  /** The messages after `after` (the ones up to it are forgotten: the client has them), if the
    * subscription is one of the user's.
    */
  def messages(user: User, id: String, after: Long): Option[Vector[LobbyMessage]] =
    lock.synchronized {
      subscriptions.get(id).filter(_.user == user).map { subscription =>
        subscription.messages = subscription.messages.filter(_.number > after)
        subscription.fetched = subscription.latest
        subscription.messages
      }
    }

  def unsubscribe(user: User, id: String): Unit =
    lock.synchronized(subscriptions.get(id).filter(_.user == user)).foreach(close)

  /** A datagram of the lobby, received on the UDP port. */
  def received(received: Lobby.Received, from: Peer): Unit = received.packet match {
    case Lobby.Packet.Hello(id, number) =>
      val accepted = lock.synchronized {
        subscriptions.get(id).filter(s => received.signedWith(s.key) && number > s.lastHello).map {
          subscription =>
            val first = subscription.peer.isEmpty
            subscription.lastHello = number
            subscription.lastSeen = now
            subscription.peer = Some(from)
            (subscription, first)
        }
      }
      accepted.foreach { (subscription, first) =>
        if first then
          subscription.gameId.foreach(gameId =>
            run(events.dispatchEvent(events.Event.UserConnectedSocket(subscription.user, gameId)))
          )
        poke(subscription)
      }
    case Lobby.Packet.Poke(_, _) => () // for the clients
  }

  private[server] def push(subscription: String, messages: Vector[MenuGameComm]): Unit =
    lock.synchronized(subscriptions.get(subscription)).foreach(push(_, messages))

  private def push(subscription: Subscription, messages: Vector[MenuGameComm]): Unit =
    if messages.nonEmpty then {
      lock.synchronized {
        for message <- messages do
          // one DataUpdated not fetched yet is as good as many (but once fetched, the client needs
          // another one: there is news since)
          if !(message == MenuGameComm.DataUpdated() && subscription.messages.lastOption.exists(
              last => last.message == message && last.number > subscription.fetched
            ))
          then {
            subscription.latest += 1
            subscription.messages =
              (subscription.messages :+ LobbyMessage(subscription.latest, message)).takeRight(
                MaxMessages
              )
          }
      }
      poke(subscription)
    }

  private def poke(subscription: Subscription): Unit =
    lock.synchronized(subscription.peer.map(_ -> subscription.latest)).foreach { (peer, latest) =>
      try
        socket.send(
          Lobby.encode(Lobby.Packet.Poke(subscription.id, latest), subscription.key),
          peer.port,
          peer.address
        )
      catch { case e: java.io.IOException => println(s"Cannot poke $peer: ${e.getMessage}") }
    }

  private def close(subscription: Subscription): Unit = {
    // (not when the client subscribed again: its new subscription is there)
    val wasConnected = lock.synchronized {
      subscription.closed = true
      subscriptions -= subscription.id
      subscription.peer.isDefined && !subscriptions.values.exists(other =>
        other.user == subscription.user && other.gameId == subscription.gameId &&
          other.peer.isDefined
      )
    }
    if wasConnected then
      subscription.gameId.foreach(gameId =>
        run(events.dispatchEvent(events.Event.UserSocketDisconnected(subscription.user, gameId)))
      )
  }

  private def sweep(): Unit =
    try {
      val limit = now - Lobby.Timeout
      lock.synchronized(subscriptions.values.filter(_.lastSeen < limit).toVector).foreach(close)
    } catch { case e: Throwable => e.printStackTrace() }
}

object LobbyServer {

  /** A client that does not fetch its messages does not get to fill the memory. */
  private val MaxMessages = 200

  /** What a subscription is told of an event: whether it is about its game (or about the games, for
    * the list of games), and what its user may know.
    */
  def messagesFor(event: events.Event, user: User, gameId: Option[String]): Vector[MenuGameComm] = {
    def ours(eventGameId: String) = gameId.contains[String](eventGameId)
    event match {
      // we are interested in this event if
      // - gameId is None, because that means we are in the list-games ui
      // - maybeGameId is None, because then it could be any game, including the one we care about
      // - maybeGameId is the exact gameId I care about
      case events.Event.GameDataRefreshed(maybeGameId)
          if gameId.forall(id => maybeGameId.forall(_ == id)) =>
        Vector(MenuGameComm.DataUpdated())
      case events.Event.GameStarted(startedGameId) if ours(startedGameId) =>
        Vector(MenuGameComm.GameStarted())
      case events.Event.HostCandidacyRequested(electionGameId) if ours(electionGameId) =>
        Vector(MenuGameComm.HostCandidacyRequested())
      case events.Event.HostElected(electedGameId, host, gameSecret, rendezvousPort)
          if ours(electedGameId) =>
        MenuGameComm.HostElected(host) +: Vector.from(
          Option.when(host == user.name)(
            MenuGameComm.SpawnGameServer(electedGameId, gameSecret, rendezvousPort)
          )
        )
      case events.Event.GameLaunchFailed(failedGameId, reason) if ours(failedGameId) =>
        Vector(MenuGameComm.GameLaunchFailed(reason))
      case events.Event.GameCredentials(creds, addresses, host, port, rendezvousPort)
          if ours(creds.gameCredentials.gameId) =>
        creds.allGameUserCredentials.find(_.userName == user.name) match {
          case Some(userCreds) =>
            // the host runs the game server: no need to go through the network
            val loopback = Option
              .when(host.contains[String](user.name))(GameServerAddress("127.0.0.1", port))
            Vector(
              MenuGameComm.HereAreYourCredentials(
                userCreds.gameId,
                userCreds.userSecret,
                (loopback.toVector ++ addresses).distinct,
                rendezvousPort
              )
            )
          case None =>
            println(
              s"I received all game credentials but it did not contain the info ${user.name}."
            )
            Vector.empty
        }
      case _ => Vector.empty // not interested
    }
  }
}
