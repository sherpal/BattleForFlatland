package communication.udp

import scala.collection.mutable

/** Two connections talking through a simulated network. */
final class ConnectionSimulationTest extends munit.FunSuite {

  private def texts(prefix: String, n: Int): List[TestMessage] =
    (0 until n).map(i => Text(s"$prefix$i")).toList

  test("reliable messages all arrive exactly once on a network losing half of the packets") {
    val net = new Pair(oneWay = 20, lossPercent = 50, jitter = 30)

    texts("a", 20).foreach(net.a.sendReliable)
    texts("b", 20).foreach(net.b.sendReliable)
    net.sim.advance(60000)

    // no ordering guarantee here, but no loss and no duplicate
    assertEquals(net.receivedByB.toSet, texts("a", 20).toSet)
    assertEquals(net.receivedByB.size, 20)
    assertEquals(net.receivedByA.toSet, texts("b", 20).toSet)
    assertEquals(net.receivedByA.size, 20)
  }

  test(
    "ordered reliable messages arrive in order, exactly once, on a lossy and reordering network"
  ) {
    val net = new Pair(oneWay = 20, lossPercent = 40, jitter = 100)

    texts("m", 30).foreach(net.a.sendOrderedReliable)
    net.sim.advance(60000)

    assertEquals(net.receivedByB.toList, texts("m", 30))
    assertEquals(net.receivedByA.toList, Nil)
  }

  test("ordered messages never go back in time on a reordering network") {
    val net = new Pair(oneWay = 20, jitter = 200)

    texts("m", 50).foreach { m =>
      net.a.sendOrdered(m)
      net.sim.advance(10)
    }
    net.sim.advance(1000)

    val indices = net.receivedByB.collect { case Text(s) => s.drop(1).toInt }.toList
    assert(indices.nonEmpty)
    assertEquals(indices, indices.sorted.distinct)
  }

  test("normal messages are lost like packets are, never duplicated") {
    val net = new Pair(oneWay = 5, lossPercent = 50)

    texts("m", 200).foreach(net.a.sendNormal)
    net.sim.advance(1000)

    val count = net.receivedByB.size
    assert(count > 50 && count < 150, s"around half of the messages should arrive, got $count")
    assertEquals(net.receivedByB.toList, net.receivedByB.toList.distinct)
  }

  test("a lossy network does not keep the connection alive forever") {
    val net = new Pair(oneWay = 20, lossPercent = 100)

    net.a.connect()
    net.a.sendReliable(Text("lost"))
    net.sim.advance(60000)

    assert(!net.a.connected)
    assertEquals(net.receivedByB.toList, Nil)
  }

  test("latency is the one way trip time, once enough pings have been exchanged") {
    val net = new Pair(oneWay = 20)

    net.a.activatePing(1000)
    net.sim.advance(15000)

    assertEquals(net.a.latency, 20)
    assertEquals(net.a.meanLatency, 20)
    assertEquals(net.b.latency, 20, "b measures it too, thanks to the pong-pong")
  }

  test("connecting keeps the connection alive through pings") {
    val net = new Pair(oneWay = 20)

    net.a.connect(pingDelay = 1000)
    net.b.connect(pingDelay = 3000)
    net.sim.advance(120000)

    assert(net.a.connected)
    assert(net.b.connected)
  }

  test("link time compensates for the difference between clocks") {
    val net      = new Pair(oneWay = 20, offsetB = 5000L)
    val computed = mutable.Buffer.empty[Long]

    net.a.computeLinkTime(sampleTime = 100, sampleNumber = 20, computed += _)
    net.sim.advance(10000)

    assertEquals(computed.toList, List(5000L))
    assertEquals(net.a.linkTime, net.sim.time + 5000L)
  }

  test("link time ignores samples spoiled by a latency spike") {
    val net      = new Pair(oneWay = 20, offsetB = 5000L)
    val computed = mutable.Buffer.empty[Long]
    net.extraDelayToA = index => if index == 5 then 600 else 0

    net.a.computeLinkTime(sampleTime = 100, sampleNumber = 20, computed += _)
    net.sim.advance(20000)

    assertEquals(computed.toList, List(5000L))
  }

  test("link time can be computed again, and the callback is called once per computation") {
    val net      = new Pair(oneWay = 20, offsetB = -300L)
    val computed = mutable.Buffer.empty[Long]

    net.a.computeLinkTime(sampleTime = 50, sampleNumber = 5, computed += _)
    net.a.activatePing(100) // pongs not asked for by the computation must not count
    net.sim.advance(10000)
    assertEquals(computed.toList, List(-300L))

    net.a.computeLinkTime(sampleTime = 50, sampleNumber = 5, computed += _)
    net.sim.advance(10000)
    assertEquals(computed.toList, List(-300L, -300L))
  }

  test("link time is also relative to the clock of the one asking") {
    val net      = new Pair(oneWay = 20, offsetB = 1000L)
    val computed = mutable.Buffer.empty[Long]

    net.b.computeLinkTime(sampleTime = 100, sampleNumber = 10, computed += _)
    net.sim.advance(10000)

    assertEquals(computed.toList, List(-1000L))
  }
}
