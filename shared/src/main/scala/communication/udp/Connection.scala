package communication.udp

import java.nio.ByteBuffer
import boopickle.CompositePickler
import boopickle.Default.*

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.collection.immutable.Queue
import scala.util.Try
import scala.util.chaining.*
import scala.util.control.NonFatal

/** Messages bigger than [[Connection.MaxDatagramSize]] once pickled are sent in several datagrams,
  * its fragments, which the peer puts back together.
  *
  * @param callback
  *   called with every message received (see [[Connection.Delivery]]). If it throws, the error is
  *   printed, and the message counts as delivered: the next ones are not held up by it.
  * @param onLost
  *   called (once per [[connect]]) when the peer has been silent for too long, see [[connected]].
  *   It is called from a timer, so possibly from another thread than the one calling [[onMessage]].
  */
class Connection[Message](
    peer: Peer,
    socket: UDPSocket,
    timers: Timers,
    callback: Message => Unit,
    onLost: () => Unit = () => ()
)(using Pickler[Message]) {
  import Connection.*

  private sealed trait InternalMessage

  private object InternalMessage {
    case class ReliableAck(id: Int) extends InternalMessage
    case class OrderedReliableAck(id: Int) extends InternalMessage
    case class Ping(time: Long) extends InternalMessage
    case class Pong(time: Long, newTime: Long) extends InternalMessage
    case class PongPong(time: Long, newTime: Long) extends InternalMessage
    case class Normal(msg: Message) extends InternalMessage
    case class Reliable(msg: Message, id: Int) extends InternalMessage
    case class Ordered(msg: Message, id: Int) extends InternalMessage
    case class OrderedReliable(msg: Message, id: Int) extends InternalMessage

    /** The `index`-th of the `count` pieces of the pickled message of the `group`. */
    case class Fragment(group: Int, index: Int, count: Int, bytes: Array[Byte])
        extends InternalMessage
  }
  import InternalMessage.*

  private given CompositePickler[InternalMessage] =
    compositePickler[InternalMessage]
      .addConcreteType[ReliableAck]
      .addConcreteType[OrderedReliableAck]
      .addConcreteType[Ping]
      .addConcreteType[Pong]
      .addConcreteType[PongPong]
      .addConcreteType[Normal]
      .addConcreteType[Reliable]
      .addConcreteType[Ordered]
      .addConcreteType[OrderedReliable]
      .addConcreteType[Fragment]

  private val latencyHistory = AtomicReference(Queue(100))

  private val pingIntervalHandle: AtomicReference[Option[Timers.TimerHandler]] = AtomicReference(
    Option.empty
  )

  private val connectionCheckHandle: AtomicReference[Option[Timers.TimerHandler]] = AtomicReference(
    Option.empty
  )

  private case class ConnectionStatus(started: Boolean, connectedCheck: Boolean, lost: Boolean)

  private val connectionStatus = AtomicReference(ConnectionStatus(false, true, false))

  /** Whether [[connect]] was called, and the peer was not (yet) deemed lost since. */
  def connected: Boolean = connectionStatus.get().pipe(status => status.started && !status.lost)

  private val nextOrderedId         = AtomicReference(0)
  private val lastReceivedOrderedId = AtomicReference(-1)

  private val nextReliableId                     = AtomicReference(0)
  private val sentReliableIds                    = AtomicReference(Set.empty[Int])
  private val receivedReliableIds: ReceivedIdSet = new ReceivedIdSet

  private val nextOrderedReliableId                     = AtomicReference(0)
  private val sentOrderedReliableIds                    = AtomicReference(Set.empty[Int])
  private val receivedOrderedReliableIds: ReceivedIdSet = new ReceivedIdSet
  private val queuedMessages               = AtomicReference(List.empty[(Int, Message)])
  private val lastDeliveredOrderedReliable = AtomicReference(-1)

  private val nextFragmentGroup = AtomicReference(0)

  /** The fragments received of the messages not complete yet: by group, their count, and the ones
    * received by index.
    */
  private val partialMessages = AtomicReference(Map.empty[Int, (Int, Map[Int, Array[Byte]])])

  /** Starts monitoring the connection: the peer is pinged every `pingDelay` ms, and deemed lost
    * (see [[connected]]) if nothing is received from it for a while, which also stops the pings and
    * the retransmission of reliable messages. Until this is called the connection can already send
    * and receive messages, but nothing watches over it. Calling it again, e.g. after the connection
    * was lost, starts over with a fresh status.
    */
  def connect(pingDelay: Int = 1000): Unit = {
    connectionStatus.set(ConnectionStatus(started = true, connectedCheck = true, lost = false))
    scheduleConnectionCheck()
    activatePing(pingDelay)
  }

  private def scheduleConnectionCheck(): Unit =
    connectionCheckHandle
      .getAndSet(Some(timers.setTimeout(ConnectionCheckDelay)(connectionCheck())))
      .foreach(_.cancel())

  // Every `ConnectionCheckDelay` ms we check that at least one message was received since last check.
  private def connectionCheck(): Unit = {
    // the update function can be retried, so the side effects are done outside of it
    val previous = connectionStatus.getAndUpdate(status =>
      status.copy(connectedCheck = false, lost = status.lost || !status.connectedCheck)
    )
    if previous.connectedCheck then scheduleConnectionCheck()
    else if !previous.lost then {
      deactivatePing()
      onLost()
    }
  }

  /** Stops monitoring the connection and sending pings, and stops retransmitting reliable messages.
    * The connection can still be used to send and receive, and [[connect]] can be called again.
    * [[onLost]] is not called.
    */
  def disconnect(): Unit = {
    connectionStatus.updateAndGet(_.copy(lost = true))
    connectionCheckHandle.getAndSet(None).foreach(_.cancel())
    deactivatePing()
  }

  def activatePing(delay: Int = 1000): Unit = {
    val handle = timers.setInterval(delay)(ping())
    pingIntervalHandle.getAndSet(Some(handle)).foreach(_.cancel())
  }

  private def deactivatePing(): Unit = pingIntervalHandle.getAndSet(None).foreach(_.cancel())

  /** To be called with every datagram received from the peer. Datagrams that cannot be decoded
    * (garbage sent to an open port, truncated packets...) are silently dropped, and do not count as
    * a sign of life of the peer.
    */
  def onMessage(array: Array[Byte]): Unit =
    Try(Unpickle[InternalMessage].fromBytes(ByteBuffer.wrap(array))).foreach { msg =>
      connectionStatus.updateAndGet(_.copy(connectedCheck = true))

      msg match {
        case Normal(actualMsg) => receivedNormalMessage(actualMsg)
        case Ordered(actualMsg, id) => receivedOrderedMessage(actualMsg, id)
        case Reliable(actualMsg, id) => receivedReliableMessage(actualMsg, id)
        case OrderedReliable(actualMsg, id) => receivedOrderedReliableMessage(actualMsg, id)
        case ReliableAck(id) => receivedReliableAck(id)
        case OrderedReliableAck(id) => receivedOrderedReliableAck(id)
        case Ping(time) => pong(time)
        case Pong(time, newTime) =>
          receivedPong(time, newTime)
          sendInternal(PongPong(newTime, timers.now()))
        case PongPong(time, newTime) => receivedPong(time, newTime)
        case fragment: Fragment => reassembled(fragment).foreach(onMessage)
      }
    }

  /** The whole message, once `fragment` is the last of its group to arrive. A group is complete
    * again when its fragments are sent again (e.g., the acknowledgement of a reliable message was
    * lost): the message is then handled again, as if received again.
    */
  private def reassembled(fragment: Fragment): Option[Array[Byte]] = {
    import fragment.*
    @tailrec
    def add(): Option[Array[Byte]] = {
      val partials        = partialMessages.get()
      val (known, pieces) = partials.getOrElse(group, (count, Map.empty[Int, Array[Byte]]))
      if known != count then None
      else {
        val withThis = pieces + (index -> bytes)
        val complete = withThis.size == count
        val updated =
          if complete then partials - group
          else {
            val withGroup = partials + (group -> (count, withThis))
            // the oldest ones are given up on
            if withGroup.size > MaxPartialMessages then withGroup - withGroup.keys.min
            else withGroup
          }
        if !partialMessages.compareAndSet(partials, updated) then add()
        else Option.when(complete)(Array.concat((0 until count).map(withThis)*))
      }
    }
    if count < 1 || count > MaxFragments || index < 0 || index >= count then None else add()
  }

  private def ping(): Unit =
    sendInternal(Ping(timers.now()))

  private def pong(time: Long): Unit =
    sendInternal(Pong(time, timers.now()))

  private def receivedPong(sendingTime: Long, midwayTime: Long): Unit = {
    val sample = (timers.now() - sendingTime).toInt / 2
    // the update function can be retried, so it must not have side effects
    latencyHistory.updateAndGet { history =>
      val withNext = history.enqueue(sample)
      if withNext.lengthCompare(10) > 0 then withNext.dequeue(1) else withNext
    }
    if linkTimeState.get().computing then
      receivedPongWhileComputingLinkTime(sendingTime, midwayTime)
  }

  private val _deltaTime = AtomicReference(0L)

  def linkTime: Long = timers.now() + _deltaTime.get()

  private case class LinkTimeState(
      computing: Boolean = false,
      trialsDone: Int = 0,
      trials: Int = 0,
      records: List[Long] = Nil,
      delay: Int = 100,
      callback: Long => Any = _ => {}
  )
  private val linkTimeState = AtomicReference(LinkTimeState())

  def computeLinkTime(
      sampleTime: Int = 100,
      sampleNumber: Int = 20,
      endCallback: Long => Any
  ): Unit = {
    linkTimeState.set(LinkTimeState(true, 0, sampleNumber, Nil, sampleTime, endCallback))

    ping()
  }

  private def receivedPongWhileComputingLinkTime(sendingTime: Long, midwayTime: Long): Unit = {
    val currentTime = timers.now()
    val latency     = (currentTime - sendingTime) / 2
    val linkTime    = midwayTime + latency
    val state = linkTimeState.updateAndGet { s =>
      s.copy(trialsDone = s.trialsDone + 1, records = (linkTime - currentTime) +: s.records)
    }

    if state.trialsDone >= state.trials then {
      linkTimeState.updateAndGet(_.copy(computing = false))

      // deltaTimeRecords = deltaTimeRecords.sorted
      val deltaTimeRecords = state.records
      val nbrRecords       = deltaTimeRecords.length
      // val median = deltaTimeRecords(nbrRecords / 2)
      val mean     = deltaTimeRecords.sum.toDouble / nbrRecords
      val variance = deltaTimeRecords.map(t => (t - mean) * (t - mean)).sum / nbrRecords
      val std      = math.sqrt(variance)
      val relevantData =
        if std < 0.0001 then deltaTimeRecords
        else
          deltaTimeRecords.filter { t =>
            // simple anomaly detection
            // we assume latency is normal distributed, which is probably wrong (chi-squared should fit better)
            // we take only data in [-x_0,x_0] where x_0 is such that P(X < x_0) < 1/20.
            val normalized = (t - mean) / std
            normalized > -1.6449 && normalized < 1.6449
          }
      if relevantData.isEmpty then {
        computeLinkTime(
          state.delay,
          state.trials,
          state.callback
        )
      } else {
        val delta = relevantData.sum / relevantData.length.toLong
        _deltaTime.set(delta)

        state.callback(delta)
      }

    } else timers.setTimeout(state.delay)(ping())
  }

  def latency: Int = latencyHistory.get().max

  def meanLatency: Int = latencyHistory.get().sum / latencyHistory.get().size

  private def sendInternal(msg: InternalMessage): Unit =
    sendInternal(msg, peer)

  private def sendInternal(msg: InternalMessage, specialPeer: Peer): Unit =
    send(datagramsOf(msg), specialPeer)

  private def send(datagrams: Vector[Array[Byte]], to: Peer): Unit =
    datagrams.foreach(socket.send(_, to.port, to.address))

  /** The datagrams that carry `msg`: itself, or its fragments when it is too big. */
  private def datagramsOf(msg: InternalMessage): Vector[Array[Byte]] = {
    val whole = pickle(msg)
    if whole.length <= MaxDatagramSize then Vector(whole)
    else {
      val group  = nextFragmentGroup.getAndUpdate(_ + 1)
      val pieces = whole.grouped(FragmentSize).toVector
      pieces.zipWithIndex.map((piece, index) =>
        pickle(Fragment(group, index, pieces.length, piece))
      )
    }
  }

  private def pickle(msg: InternalMessage): Array[Byte] = {
    val bb    = Pickle.intoBytes(msg)
    val array = new Array[Byte](bb.remaining())
    bb.get(array)
    array
  }

  def send(msg: Message, delivery: Delivery): Unit = delivery match {
    case Delivery.Normal => sendNormal(msg)
    case Delivery.Ordered => sendOrdered(msg)
    case Delivery.Reliable => sendReliable(msg)
    case Delivery.OrderedReliable => sendOrderedReliable(msg)
  }

  def sendNormal(msg: Message): Unit =
    sendInternal(Normal(msg))

  def sendReliable(msg: Message): Unit = sendReliableTo(msg, peer)

  def sendReliableTo(msg: Message, specialPeer: Peer): Unit = {
    val id = nextReliableId.getAndUpdate(_ + 1)

    sentReliableIds.updateAndGet(_ + id)

    // the same datagrams every time, so that the fragments of different sends complete each other
    val datagrams = datagramsOf(Reliable(msg, id))
    retransmitUntil(sentReliableIds.get().contains(id))(send(datagrams, specialPeer))
  }

  def sendOrderedReliable(msg: Message): Unit = {
    val id = nextOrderedReliableId.getAndUpdate(_ + 1)

    sentOrderedReliableIds.updateAndGet(_ + id)

    val datagrams = datagramsOf(OrderedReliable(msg, id))
    retransmitUntil(sentOrderedReliableIds.get().contains(id))(send(datagrams, peer))
  }

  /** Sends once, then again every few latencies for as long as `pending` holds and the peer is not
    * deemed lost.
    */
  private def retransmitUntil(pending: => Boolean)(send: => Unit): Unit = {
    send
    timers.setTimeout((4 * latency).max(MinRetransmitDelay)) {
      if pending && !connectionStatus.get().lost then retransmitUntil(pending)(send)
    }
  }

  private def receivedReliableAck(id: Int): Unit =
    sentReliableIds.updateAndGet(_ - id)

  private def receivedOrderedReliableAck(id: Int): Unit =
    sentOrderedReliableIds.updateAndGet(_ - id)

  def sendOrdered(msg: Message): Unit = {
    val id = nextOrderedId.getAndUpdate(_ + 1)
    sendInternal(Ordered(msg, id))
  }

  private def deliver(msg: Message): Unit =
    try callback(msg)
    catch {
      case NonFatal(error) =>
        println(s"Handling a message from $peer failed: $error")
        error.printStackTrace()
    }

  private def receivedNormalMessage(msg: Message): Unit = deliver(msg)

  private def receivedOrderedMessage(msg: Message, id: Int): Unit =
    if lastReceivedOrderedId.getAndUpdate(last => last.max(id)) < id then {
      deliver(msg)
    }

  private def receivedReliableMessage(msg: Message, id: Int): Unit = {
    sendInternal(ReliableAck(id))

    // discarding already received messages
    if receivedReliableIds.addIfAbsent(id) then {
      deliver(msg)
    }
  }

  private def receivedOrderedReliableMessage(msg: Message, id: Int): Unit = {
    sendInternal(OrderedReliableAck(id))

    if receivedOrderedReliableIds.addIfAbsent(id) then {
      val lastDelivered = lastDeliveredOrderedReliable.get()
      if id == lastDelivered + 1 then {
        lastDeliveredOrderedReliable.set(id)
        deliver(msg)
        service()
      } else {
        def insertMessage(
            id: Int,
            msg: Message,
            messageList: List[(Int, Message)]
        ): List[(Int, Message)] =
          if messageList.isEmpty then List((id, msg))
          else if messageList.head._1 > id then (id, msg) :: messageList
          else messageList.head :: insertMessage(id, msg, messageList.tail)

        queuedMessages.updateAndGet(messages => insertMessage(id, msg, messages))
        // in case the missing messages were delivered while we were queueing this one
        service()
      }
    }
  }

  @tailrec
  private def service(): Unit = {
    val messages = queuedMessages.get()
    val last     = lastDeliveredOrderedReliable.get()
    messages match {
      case (id, msg) :: tail if id == last + 1 =>
        if queuedMessages.compareAndSet(messages, tail) then {
          lastDeliveredOrderedReliable.set(id)
          deliver(msg)
          service()
        } else service()
      case _ => ()
    }
  }
}

object Connection {

  /** How a message is delivered:
    *   - `Normal`: sent once, may be lost, duplicated or reordered;
    *   - `Ordered`: sent once, may be lost, but never delivered after a more recent message;
    *   - `Reliable`: sent until acknowledged, delivered exactly once, in any order;
    *   - `OrderedReliable`: sent until acknowledged, delivered exactly once, in the order of
    *     sending.
    */
  enum Delivery {
    case Normal, Ordered, Reliable, OrderedReliable
  }

  private val ConnectionCheckDelay = 10000

  /** Lower bound (ms) for the delay between two sends of a reliable message, to avoid flooding when
    * the measured latency is (close to) zero.
    */
  private val MinRetransmitDelay = 20

  /** The biggest datagram (a pickled message) sent as is; bigger ones are sent in fragments. A
    * datagram bigger than what the network path carries in one packet (1500 bytes on most networks,
    * minus the headers, and as low as 1280 for IPv6) is fragmented by IP, and many NATs drop such
    * fragments (e.g., routers sending back to their local network what is sent to their public
    * address). With the envelope (see [[Envelope.DataSocket]]) and the IPv6 and UDP headers, the
    * datagrams stay below 1280 bytes.
    */
  private[udp] val MaxDatagramSize = 1150

  /** How many bytes of the message a fragment carries: with what says which fragment it is, the
    * fragment stays below [[MaxDatagramSize]].
    */
  private val FragmentSize = 1100

  /** At most so many fragments by message (about a megabyte), and so many messages put back
    * together at a time, so that the memory used by the fragments stays bounded.
    */
  private val MaxFragments       = 1024
  private val MaxPartialMessages = 32

  private class ReceivedIdSet {
    private val upToValueAndOutOfOrder =
      AtomicReference[(upToValue: Int, outOfOrder: Set[Int])]((-1, Set.empty))

    def addIfAbsent(id: Int): Boolean = {
      @tailrec
      def add(): Boolean = {
        val current @ (upToValue, outOfOrder) = upToValueAndOutOfOrder.get()
        if id <= upToValue || outOfOrder.contains(id) then false
        else {
          val updated =
            if id > upToValue then {
              if id != upToValue + 1 then {
                (upToValue, outOfOrder + id)
              } else {
                @tailrec
                def catchUpUpToValue(
                    upToValue: Int,
                    outOfOrder: Set[Int]
                ): (upToValue: Int, outOfOrder: Set[Int]) =
                  if outOfOrder.contains(upToValue + 1) then
                    catchUpUpToValue(upToValue + 1, outOfOrder - (upToValue + 1))
                  else (upToValue, outOfOrder)

                catchUpUpToValue(id, outOfOrder)
              }
            } else (upToValue, outOfOrder)
          if upToValueAndOutOfOrder.compareAndSet(current, updated) then true else add()
        }
      }
      add()
    }
  }

}
