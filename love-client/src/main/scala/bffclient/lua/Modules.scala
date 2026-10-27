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

  /** The next response, or null. */
  def poll(): HttpResponse = js.native
}

@js.native
trait HttpResponse extends js.Object {
  val id: Int = js.native

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
