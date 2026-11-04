package bffclient.menu

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import bffclient.lua.LuaUDPSocket
import communication.udp.{Hmac, Lobby as Protocol, Rendezvous, TimersImpl}
import errors.ErrorADT
import menus.data.{LobbyMessage, LobbySubscription, MenuGameComm}

/** The messages of the menu server (see `communication.udp.Lobby`): we subscribe in HTTPS, say
  * hello in UDP every few seconds (so that the menu server knows where to poke us, and that we are
  * still there), and get the messages in HTTPS when poked, at most once per
  * [[Protocol.PokeWindow]]. They are given to `onMessage` in order, once each.
  *
  * When the menu server forgets us (it restarted, or did not hear from us for a while), or stops
  * poking us, we subscribe again.
  */
final class Lobby private (
    api: MenuApi,
    gameId: Option[String],
    onMessage: MenuGameComm => Unit
)(using ExecutionContext) {
  import Lobby.*

  private val timers  = TimersImpl()
  private val socket  = LuaUDPSocket()
  private val counter = Rendezvous.Counter()

  private var subscription = Option.empty[(LobbySubscription, Hmac)]
  private var poked        = false // since subscribed
  private var handled      = 0L // the number of the latest message given to onMessage
  private var lastPoke     = 0L // when
  private var lastHello    = 0L // when
  private var fetching     = false
  private var fetchAgain   = false // poked while fetching
  private var fetchPlanned = false
  private var subscribing  = false
  private var closed       = false

  socket.setMessageCallback((datagram, from) =>
    subscription.foreach((current, key) =>
      Protocol.parseSignedWith(datagram, key) match {
        case Some(Protocol.Packet.Poke(id, latest))
            if id == current.id && from.port == current.udpPort =>
          lastPoke = now
          poked = true
          if latest > handled then planFetch()
        case _ => ()
      }
    )
  )

  // until the first poke, faster: datagrams get lost
  private val helloTimer = timers.setInterval(FirstHelloInterval) {
    if !poked || now - lastHello >= Protocol.HelloInterval then sendHello()
    if subscription.isDefined && !subscribing && now - lastPoke > SilenceLimit then {
      println("[lobby] The menu server stopped poking us: subscribing again")
      subscribe()
    }
  }

  def isOpen: Boolean = !closed

  def close(): Unit = if !closed then {
    closed = true
    helloTimer.cancel()
    socket.close()
    subscription.foreach((current, _) => api.unsubscribe(current.id))
  }

  private def now = System.currentTimeMillis()

  private def subscribe(): Future[Unit] = {
    subscribing = true
    val subscribed = api.subscribe(gameId).map { created =>
      subscribing = false
      subscription = Some(created -> Hmac(created.key))
      handled = 0
      poked = false
      lastPoke = now // (the time for its first pokes)
      if !closed then {
        sendHello()
        planFetch() // there may be messages already
      }
    }
    subscribed.failed.foreach { error =>
      subscribing = false
      println(s"[lobby] Cannot subscribe: ${error.getMessage}")
    }
    subscribed
  }

  private def sendHello(): Unit =
    for (current, key) <- subscription if !closed do {
      lastHello = now
      socket.send(
        Protocol.encode(Protocol.Packet.Hello(current.id, counter.next()), key),
        current.udpPort,
        api.host
      )
    }

  private def planFetch(): Unit =
    if fetching then fetchAgain = true
    else if !fetchPlanned then {
      fetchPlanned = true
      timers.setTimeout(Protocol.PokeWindow)(fetch())
    }

  private def fetch(): Unit = {
    fetchPlanned = false
    for (current, _) <- subscription if !closed do {
      fetching = true
      api.lobbyMessages(current.id, handled).onComplete { result =>
        fetching = false
        result match {
          case Success(messages) if subscription.exists(_._1.id == current.id) =>
            deliver(messages)
          case Success(_) => () // subscribed again meanwhile
          case Failure(error: ApiError) if error.error == ErrorADT.RawNotFound() =>
            println("[lobby] The menu server forgot our subscription: subscribing again")
            fetchAgain = false
            if !closed then subscribe()
          case Failure(error) => println(s"[lobby] Cannot get the messages: ${error.getMessage}")
        }
        if fetchAgain then {
          fetchAgain = false
          planFetch()
        }
      }
    }
  }

  private def deliver(messages: Vector[LobbyMessage]): Unit =
    for message <- messages.sortBy(_.number) if message.number > handled && !closed do {
      handled = message.number
      onMessage(message.message)
    }
}

object Lobby {

  /** Hellos until the first poke, then every [[Protocol.HelloInterval]]. */
  private val FirstHelloInterval = 1000

  /** Without a poke for this long (we say hello every [[Protocol.HelloInterval]], each answered by
    * a poke), something is wrong.
    */
  private val SilenceLimit = 3 * Protocol.HelloInterval + 1000

  /** Completes once subscribed (it fails if the menu server cannot be reached). */
  def open(api: MenuApi, gameId: Option[String], onMessage: MenuGameComm => Unit)(using
      ExecutionContext
  ): Future[Lobby] = {
    val lobby = new Lobby(api, gameId, onMessage)
    lobby
      .subscribe()
      .transform(
        _ => lobby,
        error => {
          lobby.close()
          error
        }
      )
  }
}
