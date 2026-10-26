package communication.udp

import java.util.concurrent.{CountDownLatch, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger

// Real time is involved, so the checks are generous. The server relies on this implementation only.
final class TimersImplTest extends munit.FunSuite {

  private val timers = new TimersImpl

  test("a timeout fires once, after the delay") {
    val latch   = new CountDownLatch(1)
    val fired   = new AtomicInteger(0)
    val started = System.nanoTime()

    timers.setTimeout(100) {
      fired.incrementAndGet()
      latch.countDown()
    }

    assert(latch.await(5, TimeUnit.SECONDS))
    assert((System.nanoTime() - started) / 1000000 >= 90)
    Thread.sleep(200)
    assertEquals(fired.get(), 1)
  }

  test("a cancelled timeout does not fire") {
    val fired = new AtomicInteger(0)

    timers.setTimeout(100)(fired.incrementAndGet()).cancel()
    Thread.sleep(300)

    assertEquals(fired.get(), 0)
  }

  test("an interval fires repeatedly until cancelled") {
    val fired = new AtomicInteger(0)
    val latch = new CountDownLatch(3)

    val handle = timers.setInterval(20) {
      fired.incrementAndGet()
      latch.countDown()
    }
    assert(latch.await(5, TimeUnit.SECONDS))
    handle.cancel()
    Thread.sleep(100) // lets a run in progress finish
    val afterCancel = fired.get()
    Thread.sleep(200)

    assertEquals(fired.get(), afterCancel)
  }

  test("timers with short delays do not need a thread each") {
    val latch = new CountDownLatch(500)
    val threads = java.util.concurrent.ConcurrentHashMap.newKeySet[Thread]()

    for _ <- 1 to 500 do timers.setTimeout(1) {
      threads.add(Thread.currentThread())
      latch.countDown()
    }

    assert(latch.await(5, TimeUnit.SECONDS))
    assert(threads.size() <= 2, s"callbacks ran on ${threads.size()} threads")
  }

  test("a failing callback does not prevent the next ones, nor an interval from going on") {
    val failures = new AtomicInteger(0)
    val latch    = new CountDownLatch(3)

    timers.setTimeout(10)(throw new IllegalStateException("expected by the test"))
    val handle = timers.setInterval(20) {
      latch.countDown()
      if failures.incrementAndGet() == 1 then throw new IllegalStateException("expected by the test")
    }

    assert(latch.await(5, TimeUnit.SECONDS))
    handle.cancel()
  }

  test("timers do not keep the JVM alive") {
    // the scheduler thread must be a daemon, or the server could not stop
    val latch = new CountDownLatch(1)
    var daemon = false
    timers.setTimeout(1) {
      daemon = Thread.currentThread().isDaemon
      latch.countDown()
    }
    assert(latch.await(5, TimeUnit.SECONDS))
    assert(daemon)
  }
}
