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
  * server's threads while it lasts. The menus' calls (to `/api/`, from someone logged in: with a
  * `session` cookie) have far more, so that playing never gets close: since anyone can send that
  * cookie, it is still a limit.
  *
  * The requests refused are logged, once a minute at most for each visitor.
  *
  * This is against one computer that floods the server, not against a real DDoS, from many
  * addresses (see deploy.md).
  */
final class RequestLimits(next: HttpHandler, limits: RequestLimits.Limits = RequestLimits.Limits())
    extends HttpHandler {
  import RequestLimits.*

  def handleRequest(exchange: HttpServerExchange): Unit = {
    val address = visitor(exchange)
    val path    = exchange.getRequestPath
    val kind =
      if isDownload(path) then Kind.Download
      else if path.startsWith("/api/") && exchange.getRequestCookie("session") != null then
        Kind.Menu
      else Kind.Other
    limits.admit(address, kind, System.nanoTime()) match {
      case Some(Refusal(retryAfter, limit, log)) =>
        if log then println(s"[limits] Too many requests from $address ($limit), at $path")
        exchange.setStatusCode(429)
        exchange.getResponseHeaders.put(Headers.RETRY_AFTER, retryAfter.toString)
        exchange.getResponseHeaders.put(Headers.CONTENT_TYPE, "text/plain; charset=utf-8")
        exchange.getResponseSender.send(s"Too many requests: try again in $retryAfter seconds.")
      case None =>
        if kind == Kind.Download then
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

  /** What a request is, for its limits. */
  enum Kind {
    case Download, Menu, Other
  }

  /** A request refused: the seconds after which to come back, the limit reached, and whether to log
    * it (the visitor's first in a minute).
    */
  final case class Refusal(retryAfter: Long, limit: String, log: Boolean)

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
    * website open, is far from it), 600 and 100 per second for the menus' calls, 60 downloads at
    * once and then one every 10 seconds (after a deploy, each client downloads the new game server,
    * and the Java runtime with it the first time: four players behind the same address, who
    * download the game too, are at 12), 6 at the same time (each keeps one of the server's threads,
    * of which Undertow has 8 per processor, 16 at least).
    */
  final class Limits(
      requests: Rate = Rate(burst = 120, perSecond = 20),
      menus: Rate = Rate(burst = 600, perSecond = 100),
      downloads: Rate = Rate(burst = 60, perSecond = 1.0 / 10),
      concurrentDownloads: Int = 6
  ) {

    /** `None` if the request may go on, or why not. */
    def admit(address: String, kind: Kind, now: Long): Option[Refusal] = {
      forgetIdle(now)
      val visitor = visitors.computeIfAbsent(address, _ => Visitor(now))
      visitor.synchronized {
        if visitor.forgotten then admit(address, kind, now) // forgotten in the meantime
        else {
          visitor.refill(now)
          val requests = if kind == Kind.Menu then visitor.menus else visitor.requests
          val refused =
            if !requests.hasOne then Some((requests.secondsToOne, s"$kind requests"))
            else if kind == Kind.Download && !visitor.downloads.hasOne then
              Some((visitor.downloads.secondsToOne, "downloads"))
            else if kind == Kind.Download && visitor.ongoingDownloads >= concurrentDownloads then
              Some((secondsAfterDownloads, "simultaneous downloads"))
            else None
          refused match {
            case Some((retryAfter, limit)) =>
              val log = visitor.lastLogged.forall(now - _ >= 60e9)
              if log then visitor.lastLogged = Some(now)
              Some(Refusal(retryAfter, limit, log))
            case None =>
              requests.take()
              if kind == Kind.Download then {
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
      val requests: Bucket         = Bucket(Limits.this.requests, now)
      val menus: Bucket            = Bucket(Limits.this.menus, now)
      val downloads: Bucket        = Bucket(Limits.this.downloads, now)
      var ongoingDownloads: Int    = 0
      var lastLogged: Option[Long] = None
      var forgotten: Boolean       = false

      def refill(now: Long): Unit = Seq(requests, menus, downloads).foreach(_.refill(now))
      def idle: Boolean =
        requests.full && menus.full && downloads.full && ongoingDownloads == 0
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
            visitor.refill(now)
            if visitor.idle then {
              visitor.forgotten = true
              visitors.remove(address, visitor)
            }
          }
        }
    }
  }
}
