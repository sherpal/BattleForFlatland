package communication.udp

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/** SHA-256: LÖVE's (`love.data.hash`, native code) in the client, which runs a lot of them (every
  * datagram of the game is authenticated, see [[Hmac]]), the portable one elsewhere (the tests, on
  * Node.js).
  */
object Sha256 {
  private lazy val inLove: Boolean = js.typeOf(js.Dynamic.global.love) != "undefined"

  def digest(data: Array[Byte]): Array[Byte] =
    if inLove then {
      val input = LuaBytes.bytesToString(data)
      LuaBytes.stringToBytes(
        if LoveVersion._version_major >= 12 then LoveData.hash("string", "sha256", input)
        else LoveData.hash("sha256", input)
      )
    } else PortableSha256.digest(data)

  @js.native @JSGlobal("love.data")
  private object LoveData extends js.Object {
    def hash(function: String, data: String): String                    = js.native
    def hash(container: String, function: String, data: String): String = js.native
  }

  @js.native @JSGlobal("love")
  private object LoveVersion extends js.Object {
    val _version_major: Int = js.native
  }

  /** The helpers of the runtime of the Lua linker (a Lua string is a sequence of bytes). */
  @js.native @JSGlobal("scalalove")
  private object LuaBytes extends js.Object {
    def stringToBytes(s: String): Array[Byte]     = js.native
    def bytesToString(bytes: Array[Byte]): String = js.native
  }
}
