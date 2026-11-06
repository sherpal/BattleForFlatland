package server

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.Headers

/** Limits on what each visitor (each IP address) asks of the menu server, so that no one can take
  * it for themselves: past them, a 429 (Too Many Requests), until they slow down.
  *
  * Each visitor has a number of requests (and fewer downloads: the client packages, the game server
  * and its runtimes, tens of MB each) that they can make at once, and which comes back with time.
  * Downloads are also limited in how many run at the same time: a slow one keeps one of the
  * server's threads while it lasts.
  *
  * This is against one computer that floods the server, not against a real DDoS, from many
  * addresses (see deploy.md).
  */
final class RequestLimits(next: HttpHandler, limits: RequestLimits.Limits = RequestLimits.Limits())
    extends HttpHandler {
  import RequestLimits.*

  def handleRequest(exchange: HttpServerExchange): Unit = {
    val address  = visitor(exchange)
    val download = isDownload(exchange.getRequestPath)
    limits.admit(address, download, System.nanoTime()) match {
      case Some(retryAfter) =>
        exchange.setStatusCode(429)
        exchange.getResponseHeaders.put(Headers.RETRY_AFTER, retryAfter.toString)
        exchange.getResponseHeaders.put(Headers.CONTENT_TYPE, "text/plain; charset=utf-8")
        exchange.getResponseSender.send(s"Too many requests: try again in $retryAfter seconds.")
      case None =>
        if download then
          exchange.addExchangeCompleteListener { (_, nextListener) =>
            limits.downloadDone(address)
            nextListener.proceed()
          }
        next.handleRequest(exchange)
    }
  }
}

object RequestLimits {

  /** `burst` at once, and then `perSecond`. */
  final case class Rate(burst: Int, perSecond: Double)

  /** The routes of the big files. */
  def isDownload(path: String): Boolean =
    path.startsWith("/api/bff/client/") ||
      path == "/api/bff/game-server/jar" ||
      path == "/api/bff/game-server/runtime"

  /** The visitor's IP address: the one that Caddy gives (in `X-Forwarded-For`) when the request
    * comes from it (from the loopback, see deploy.md), the one of the connection otherwise. Caddy
    * does not keep the `X-Forwarded-For` that the visitor sends: the last address is the one it
    * saw.
    */
  def visitor(exchange: HttpServerExchange): String = {
    val source = Option(exchange.getSourceAddress).map(_.getAddress)
    val forwarded = Option(exchange.getRequestHeaders.getLast(Headers.X_FORWARDED_FOR))
      .flatMap(_.split(',').map(_.trim).filter(_.nonEmpty).lastOption)
    source match {
      case Some(address) if address.isLoopbackAddress => forwarded.getOrElse(address.getHostAddress)
      case Some(address) => address.getHostAddress
      case None => forwarded.getOrElse("unknown")
    }
  }

  /** What each visitor has left (with the time in nanoseconds, given, for the tests).
    *
    * By default, 120 requests at once and 20 per second (a family behind the same address, with the
    * game and the website open, is far from it), 10 downloads at once and then one every two
    * minutes, 3 at the same time.
    */
  final class Limits(
      requests: Rate = Rate(burst = 120, perSecond = 20),
      downloads: Rate = Rate(burst = 10, perSecond = 1.0 / 120),
      concurrentDownloads: Int = 3
  ) {

    /** `None` if the request may go on, or the seconds after which to come back. */
    def admit(address: String, download: Boolean, now: Long): Option[Long] = {
      forgetIdle(now)
      val visitor = visitors.computeIfAbsent(address, _ => Visitor(now))
      visitor.synchronized {
        if visitor.forgotten then admit(address, download, now) // forgotten in the meantime
        else {
          visitor.requests.refill(now)
          visitor.downloads.refill(now)
          if !visitor.requests.hasOne then Some(visitor.requests.secondsToOne)
          else if download && !visitor.downloads.hasOne then Some(visitor.downloads.secondsToOne)
          else if download && visitor.ongoingDownloads >= concurrentDownloads then
            Some(secondsAfterDownloads)
          else {
            visitor.requests.take()
            if download then {
              visitor.downloads.take()
              visitor.ongoingDownloads += 1
            }
            None
          }
        }
      }
    }

    /** A download that [[admit]] let go is over. (Its visitor is not forgotten until then.) */
    def downloadDone(address: String): Unit =
      Option(visitors.get(address)).foreach { visitor =>
        visitor.synchronized(visitor.ongoingDownloads = math.max(visitor.ongoingDownloads - 1, 0))
      }

    /** How many visitors are remembered. */
    def size: Int = visitors.size

    private final class Bucket(rate: Rate, now: Long) {
      private var tokens = rate.burst.toDouble
      private var last   = now

      def refill(now: Long): Unit = {
        tokens = math.min(rate.burst.toDouble, tokens + (now - last) / 1e9 * rate.perSecond)
        last = now
      }
      def hasOne: Boolean    = tokens >= 1
      def take(): Unit       = tokens -= 1
      def full: Boolean      = tokens >= rate.burst
      def secondsToOne: Long = math.max(math.ceil((1 - tokens) / rate.perSecond).toLong, 1)
    }

    private final class Visitor(now: Long) {
      val requests: Bucket      = Bucket(Limits.this.requests, now)
      val downloads: Bucket     = Bucket(Limits.this.downloads, now)
      var ongoingDownloads: Int = 0
      var forgotten: Boolean    = false
    }

    private val secondsAfterDownloads = 30L

    private val visitors = ConcurrentHashMap[String, Visitor]()

    /** When the visitors were last looked through (none yet: Long.MinValue). */
    private val lastForgetting = AtomicLong(Long.MinValue)

    /** Once a minute, forgets the visitors who have all they can have (they would start again the
      * same), so that the memory does not grow with the addresses seen.
      */
    private def forgetIdle(now: Long): Unit = {
      val last = lastForgetting.get
      if last == Long.MinValue then lastForgetting.compareAndSet(last, now)
      else if now - last >= 60e9 && lastForgetting.compareAndSet(last, now) then
        visitors.forEach { (address, visitor) =>
          visitor.synchronized {
            visitor.requests.refill(now)
            visitor.downloads.refill(now)
            if visitor.requests.full && visitor.downloads.full && visitor.ongoingDownloads == 0
            then {
              visitor.forgotten = true
              visitors.remove(address, visitor)
            }
          }
        }
    }
  }
}
