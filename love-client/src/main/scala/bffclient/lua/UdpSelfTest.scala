package bffclient.lua

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js
import scala.collection.mutable

import boopickle.CompositePickler
import boopickle.Default.*
import communication.udp.*
import communication.udp.Connection.Delivery

import bffclient.love.Event

/** Checks, in LÖVE, that [[LuaUDPSocket]] works with real sockets (on the loopback interface), and
  * with [[Connection]] on top of it. Run by `tools/love-udp-test.sh`, headless: prints what it
  * does, and quits with a failure code if anything is wrong.
  */
object UdpSelfTest {
  given ExecutionContext = ExecutionContext.global

  private def fail(message: String): Nothing = throw new AssertionError(message)

  private def check(condition: Boolean, message: String): Unit = {
    if !condition then fail(message)
    println(s"  ok: $message")
  }

  private def within[A](what: String, ms: Int = 3000)(future: Future[A]): Future[A] = {
    val p = Promise[A]()
    val handle =
      js.timers.setTimeout(ms.toDouble)(p.tryFailure(new AssertionError(s"timeout: $what")))
    future.onComplete { result =>
      js.timers.clearTimeout(handle); p.tryComplete(result)
    }
    p.future
  }

  private sealed trait Msg
  private case class Text(value: String) extends Msg
  private object Msg {
    given CompositePickler[Msg] = compositePickler[Msg].addConcreteType[Text]
  }

  private def listening(): LuaUDPSocket = {
    val socket = new LuaUDPSocket
    socket.bind(0, Some("127.0.0.1"))
    socket
  }

  private def nextDatagram(socket: LuaUDPSocket): Future[(Array[Byte], Peer)] = {
    val p = Promise[(Array[Byte], Peer)]()
    socket.setMessageCallback((bytes, from) => p.trySuccess((bytes, from)))
    p.future
  }

  private def datagramsTravel(): Future[Unit] = {
    println("Datagrams travel between sockets, with the address of the sender")
    val a = listening()
    val b = listening()
    check(
      a.peer.port > 0 && a.peer.port != b.peer.port,
      s"the system chose ports ${a.peer.port} and ${b.peer.port}"
    )
    check(a.peer.address == "127.0.0.1", "bound to the given address")
    val received = nextDatagram(b)
    a.send(Array[Byte](1, 2, 3), b.peer.port, "127.0.0.1")
    within("a datagram")(received).map { (bytes, from) =>
      check(bytes.toList == List[Byte](1, 2, 3), "content received")
      check(from == a.peer, s"sender is $from")
      a.close(); b.close()
    }
  }

  private def anyByteTravels(): Future[Unit] = {
    println("Every byte value travels untouched, including 0, and empty datagrams")
    val a    = listening()
    val b    = listening()
    val all  = Array.tabulate[Byte](256)(_.toByte)
    val sent = mutable.Buffer[List[Byte]](all.toList, Nil, List(0), List(-1, 0, -1))
    val got  = mutable.Buffer.empty[List[Byte]]
    val done = Promise[Unit]()
    b.setMessageCallback { (bytes, _) =>
      got += bytes.toList
      if got.size == sent.size then done.trySuccess(())
    }
    sent.foreach(d => a.send(d.toArray, b.peer.port, "127.0.0.1"))
    within("all datagrams")(done.future).map { _ =>
      check(got.toList == sent.toList, "all content received, in order (on the loopback interface)")
      a.close(); b.close()
    }
  }

  private def hostNamesAreResolved(): Future[Unit] = {
    println("Host names can be used")
    val a        = new LuaUDPSocket
    val b        = listening()
    val received = nextDatagram(b)
    a.send(Array[Byte](7), b.peer.port, "localhost") // also gets a port, and starts receiving
    within("via localhost")(received).map { (bytes, _) =>
      check(bytes.toList == List[Byte](7), "datagram sent to localhost received")
      // A client that never bound gets answers
      val answer = nextDatagram(a)
      b.send(Array[Byte](8), a.peer.port, "127.0.0.1")
      within("the answer")(answer).map { (bytes, _) =>
        check(bytes.toList == List[Byte](8), "unbound socket receives what is sent to its port")
        a.close(); b.close()
      }
    }.flatten
  }

  private def badDestinationDoesNotThrow(): Future[Unit] = {
    println("Sending to a destination that cannot be resolved does not fail")
    val a = listening()
    a.send(Array[Byte](1), 1234, "no-such-host.invalid")
    a.send(Array[Byte](1), 1234, "no-such-host.invalid")
    a.close()
    a.send(Array[Byte](1), 1234, "127.0.0.1")
    check(true, "no exception")
    Future.unit
  }

  private def closing(): Future[Unit] = {
    println("Closing tells so")
    val a      = listening()
    var closed = 0
    a.setCloseCallback(() => closed += 1)
    a.close()
    a.close()
    check(closed == 1, "close callback called once")
    Future.unit
  }

  private def bindingTwiceFails(): Future[Unit] = {
    println("Binding a port in use fails")
    val a = listening()
    val failed =
      try { listening2(a.peer.port); false }
      catch { case _: java.io.IOException => true }
    check(failed, "IOException")
    a.close()
    Future.unit
  }

  private def listening2(port: Int): LuaUDPSocket = {
    val socket = new LuaUDPSocket
    socket.bind(port, Some("127.0.0.1"))
    socket
  }

  private def connectionsWork(): Future[Unit] = {
    println("Connections work on top of it, through the envelope, with the real timers")
    import Msg.given
    val clientSocket   = listening()
    val serverSocket   = listening()
    val timers         = new TimersImpl
    val received       = mutable.Buffer.empty[Msg]
    val serverGot      = Promise[Unit]()
    val clientGot      = Promise[Unit]()
    val clientReceived = mutable.Buffer.empty[Msg]

    val client = new Connection[Msg](
      serverSocket.peer,
      new Envelope.DataSocket(clientSocket),
      timers,
      m => { clientReceived += m; clientGot.trySuccess(()); () }
    )
    val server = new Connection[Msg](
      clientSocket.peer,
      new Envelope.DataSocket(serverSocket),
      timers,
      m => { received += m; if received.size == 3 then serverGot.trySuccess(()); () }
    )
    def route(socket: LuaUDPSocket, connection: Connection[Msg]): Unit =
      socket.setMessageCallback { (bytes, _) =>
        Envelope.parse(bytes).foreach {
          case Envelope.Packet.Data(payload) => connection.onMessage(payload)
          case _ => ()
        }
      }
    route(clientSocket, client)
    route(serverSocket, server)
    client.connect()
    server.connect()

    client.send(Text("one"), Delivery.OrderedReliable)
    client.send(Text("two"), Delivery.OrderedReliable)
    client.send(Text("three"), Delivery.Reliable)
    within("the messages")(serverGot.future)
      .flatMap { _ =>
        check(
          received.take(2).toList == List(Text("one"), Text("two")),
          "ordered reliable messages arrive in order"
        )
        server.send(Text("back"), Delivery.OrderedReliable)
        within("the answer")(clientGot.future)
      }
      .flatMap { _ =>
        check(clientReceived.toList == List(Text("back")), "the answer arrives")
        val linkTime = Promise[Long]()
        client.computeLinkTime(sampleTime = 20, sampleNumber = 5, linkTime.trySuccess)
        within("link time", 5000)(linkTime.future)
      }
      .map { delta =>
        check(math.abs(delta) < 100, s"same machine, same clock: link time delta is $delta ms")
        client.disconnect(); server.disconnect()
        clientSocket.close(); serverSocket.close()
      }
  }

  def main(args: Array[String]): Unit = {
    val steps = List[() => Future[Unit]](
      () => datagramsTravel(),
      () => anyByteTravels(),
      () => hostNamesAreResolved(),
      () => badDestinationDoesNotThrow(),
      () => closing(),
      () => bindingTwiceFails(),
      () => connectionsWork()
    )
    steps
      .foldLeft(Future.unit)((previous, step) => previous.flatMap(_ => step()))
      .onComplete {
        case scala.util.Success(_) =>
          println("UDP SELF TEST PASSED")
          Event.quit(0)
        case scala.util.Failure(error) =>
          println(s"UDP SELF TEST FAILED: ${error.getMessage}")
          Event.quit(1)
      }
  }
}
