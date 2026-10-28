package bffclient.lua

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import bffclient.love.Image

/** The WebSocket client written in Lua (src/main/lua/websocket.lua). */
@js.native @JSImport("websocket", JSImport.Namespace)
object WebSocketModule extends js.Object {
  def connect(url: String): (socket: WebSocket, error: String) = js.native
  def connect(url: String, timeout: Double, cookie: String): (socket: WebSocket, error: String) =
    js.native
}

/** Non-blocking HTTP (src/main/lua/http.lua). */
@js.native @JSImport("http", JSImport.Namespace)
object HttpModule extends js.Object {

  /** Returns the id of the request. */
  def request(method: String, url: String, body: String, cookie: String): Int = js.native

  /** Writes the body of the response to `file` (a path of the operating system), only if the status
    * is 200 (the response's `body` is then empty). Its progress is polled too.
    */
  @JSName("request")
  def requestFile(method: String, url: String, body: String, cookie: String, file: String): Int =
    js.native

  /** The next response, or null. */
  def poll(): HttpResponse = js.native
}

/** A response, or the progress of a download (when `received` is defined, the other fields then
  * being undefined).
  */
@js.native
trait HttpResponse extends js.Object {
  val id: Int = js.native

  /** The bytes received so far, by a download. */
  val received: js.UndefOr[Double] = js.native

  /** 0 if the request failed (see `error`). */
  val status: Int       = js.native
  val body: String      = js.native
  val setCookie: String = js.native
  val error: String     = js.native
}

@js.native
trait WebSocket extends js.Object {

  /** Sends a binary message (a Lua string). */
  def send(data: String): Unit = js.native

  /** Next received message, or null. Never blocks. */
  def poll(): String = js.native

  def isOpen(): Boolean = js.native
  def close(): Unit     = js.native
}

/** A module whose inputs are handled one after the other by a LÖVE thread, see
  * `workers.module` (src/main/lua/workers.lua). Use `hosting.WorkerResults` rather than this.
  *
  * @tparam I
  *   what the thread is given
  * @tparam R
  *   what it gives back
  */
@js.native
trait WorkerModule[I, R] extends js.Object {

  /** Gives the input to the thread; returns the id of its result. */
  def run(input: I): Int = js.native

  /** The next result, if there is one. */
  def poll(): js.UndefOr[WorkerResult[R]] = js.native
}

@js.native
trait WorkerResult[R] extends js.Object {
  val id: Int = js.native

  /** Meaningless if there is an `error`. */
  val result: R = js.native

  /** Empty when it worked. */
  val error: String = js.native
}

/** Shell commands (src/main/lua/process.lua): the result of a command is its output (stdout and
  * stderr, together).
  */
@js.native @JSImport("process", JSImport.Namespace)
object ProcessModule extends WorkerModule[String, String]

/** SHA-256 of files (src/main/lua/digest.lua): of a path of the operating system, in hexadecimal
  * (lower case).
  */
@js.native @JSImport("digest", JSImport.Namespace)
object DigestModule extends WorkerModule[String, String]

/** Non-blocking UDP sockets (src/main/lua/udp.lua). Use [[LuaUDPSocket]] rather than this. */
@js.native @JSImport("udp", JSImport.Namespace)
object UdpModule extends js.Object {
  @JSName("new")
  def create(): UdpHandle = js.native
}

@js.native
trait UdpHandle extends js.Object {

  /** `ok` is false when the address cannot be bound, then `error` says why. A port 0 lets the
    * system choose, an address null means all of them.
    */
  def bind(port: Int, address: String): (ok: Boolean, error: String) = js.native

  def getsockname(): (ip: String, port: Int) = js.native

  /** Sends a datagram (a Lua string) to an IP address or a host name; `ok` is false if it could not
    * be sent, then `error` says why.
    */
  def send(data: String, host: String, port: Int): (ok: Boolean, error: String) = js.native

  /** The next datagram received, if there is one. Never blocks. */
  def poll(): js.UndefOr[UdpDatagram] = js.native

  def close(): Unit = js.native
}

/** A datagram received, with the address of its sender. */
@js.native
trait UdpDatagram extends js.Object {
  val data: String = js.native
  val ip: String   = js.native
  val port: Int    = js.native
}

/** Helpers of the ScalaLove runtime. */
@js.native @JSGlobal("scalalove")
object ScalaLove extends js.Object {
  def stringToBytes(s: String): Array[Byte]     = js.native
  def bytesToString(bytes: Array[Byte]): String = js.native

  /** A Lua sequence (1-based table, as returned by Lua functions) as a JS array. */
  def fromLuaSequence[A](sequence: LuaSequence[A]): js.Array[A] = js.native
  def toLuaSequence[A](array: js.Array[A]): LuaSequence[A]      = js.native
}

/** A Lua table with elements at indices 1 to n (convert it with `ScalaLove.fromLuaSequence`). */
@js.native
trait LuaSequence[A] extends js.Object

/** LuaJIT's `jit` module. */
@js.native @JSGlobal("jit")
object Jit extends js.Object {

  /** The processor's architecture: "x86", "x64", "arm", "arm64", ... */
  val arch: String = js.native
}

@js.native @JSGlobal("os")
object Os extends js.Object {
  def getenv(name: String): String = js.native
}

/** Images, from src/main/lua/images.lua. */
@js.native @JSImport("images", JSImport.Namespace)
object Images extends js.Object {

  /** The image at this path of the game directory, or null if there is none. */
  def load(path: String): Image = js.native
}
