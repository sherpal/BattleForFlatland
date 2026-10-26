package communication.udp

import boopickle.CompositePickler
import boopickle.Default.*
import scala.collection.mutable

// Everything in here (and in the tests using it) deliberately avoids threads, real time, sockets and
// java.util.Random: the udp code is shared between the JVM (server) and Lua (client), so its tests
// should only need what both platforms provide. Time is simulated, and so is the network.

private[udp] sealed trait TestMessage
private[udp] case class Text(value: String) extends TestMessage

private[udp] object TestMessage {
  given CompositePickler[TestMessage] = compositePickler[TestMessage].addConcreteType[Text]
}

/** A virtual clock together with the timers scheduled on it. Nothing happens until [[advance]] is called. */
private[udp] final class Simulation {
  private final class Task(val due: Long, val seq: Long, val run: () => Unit) {
    var cancelled = false
  }

  private val queue = mutable.PriorityQueue.empty[Task](using Ordering.by((t: Task) => (-t.due, -t.seq)))
  private var seq   = 0L

  var time: Long = 0L

  def schedule(delay: Int)(callback: => Unit): Timers.TimerHandler = {
    val task = new Task(time + delay, seq, () => callback)
    seq += 1
    queue.enqueue(task)
    Timers.TimerHandler(() => task.cancelled = true)
  }

  def every(period: Int)(callback: => Unit): Timers.TimerHandler = {
    var stopped = false
    def tick(): Unit = {
      schedule(period) {
        if !stopped then {
          callback
          tick()
        }
      }
      ()
    }
    tick()
    Timers.TimerHandler(() => stopped = true)
  }

  /** Moves the time forward, running everything that is due on the way (in chronological order). */
  def advance(ms: Int): Unit = {
    val target = time + ms
    while queue.nonEmpty && queue.head.due <= target do {
      val task = queue.dequeue()
      time = task.due
      if !task.cancelled then task.run()
    }
    time = target
  }
}

/** Timers of one machine; `offset` is how far its clock is ahead of the simulation's. */
private[udp] final class FakeTimers(sim: Simulation, offset: Long = 0L) extends Timers {
  override def now(): Long = sim.time + offset

  override def setTimeout(time: Int)(callback: => Unit): Timers.TimerHandler =
    sim.schedule(time)(callback)

  override def setInterval(time: Int)(callback: => Unit): Timers.TimerHandler =
    sim.every(time)(callback)
}

private[udp] final class FakeSocket(val peer: Peer, onSend: Array[Byte] => Unit = _ => ())
    extends UDPSocket {
  val sent = mutable.Buffer.empty[(bytes: Array[Byte], port: Int, address: String)]

  override def bind(port: Int, address: Option[String]): Unit = ()

  override def send(message: Array[Byte], port: Int, address: String): Unit = {
    sent += ((message.clone(), port, address))
    onSend(message.clone())
  }

  override def close(): Unit = ()

  def takeSent(): Array[Byte] = sent.remove(0).bytes
}

/** A single connection whose packets are inspected and delivered by hand. */
private[udp] final class Harness(peer: Peer = Peer("localhost", 1)) {
  import TestMessage.given

  val sim      = new Simulation
  val timers   = new FakeTimers(sim)
  val socket   = new FakeSocket(peer)
  val received = mutable.Buffer.empty[TestMessage]
  val conn =
    new Connection[TestMessage](peer, socket, timers, message => { received += message; () })
}

/** Two connections linked by a simulated network, with a one way latency of `oneWay` ms and, for every
  * packet, `lossPercent`% chances of being lost and a random extra delay in `[0, jitter]` ms (which
  * reorders packets). The "random" numbers are deterministic. B's clock is `offsetB` ms ahead of A's.
  */
private[udp] final class Pair(
    oneWay: Int = 20,
    lossPercent: Int = 0,
    jitter: Int = 0,
    offsetB: Long = 0L,
    seed: Long = 42L
) {
  import TestMessage.given

  val sim         = new Simulation
  val receivedByA = mutable.Buffer.empty[TestMessage]
  val receivedByB = mutable.Buffer.empty[TestMessage]

  /** Extra delay (ms) of the n-th packet (counting from 0) sent towards A, to simulate a latency spike. */
  var extraDelayToA: Int => Int = _ => 0

  private var packetsToA = 0
  private var random     = seed

  private def nextRandom(bound: Int): Int = {
    random = (random * 1103515245L + 12345L) & 0x7fffffffL
    ((random >> 8) % bound).toInt
  }

  private def transmit(toA: Boolean, bytes: Array[Byte]): Unit =
    if nextRandom(100) >= lossPercent then {
      val extra = if toA then {
        val index = packetsToA
        packetsToA += 1
        extraDelayToA(index)
      } else 0
      val delay = oneWay + extra + (if jitter > 0 then nextRandom(jitter + 1) else 0)
      sim.schedule(delay)(if toA then a.onMessage(bytes) else b.onMessage(bytes))
      ()
    }

  private val socketA = new FakeSocket(Peer("a", 1), transmit(toA = false, _))
  private val socketB = new FakeSocket(Peer("b", 2), transmit(toA = true, _))

  lazy val a = new Connection[TestMessage](
    socketB.peer,
    socketA,
    new FakeTimers(sim),
    message => { receivedByA += message; () }
  )
  lazy val b = new Connection[TestMessage](
    socketA.peer,
    socketB,
    new FakeTimers(sim, offsetB),
    message => { receivedByB += message; () }
  )

  // connections are lazy only because of their circular dependency through the network
  a
  b
}
