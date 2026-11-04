package communication.udp

import java.nio.charset.StandardCharsets

/** HMAC-SHA256 (RFC 2104), which authenticates the datagrams of the game (see [[Envelope]],
  * [[Rendezvous]] and [[Lobby]]): only who knows the key can make a tag that matches.
  *
  * The SHA-256 is the platform's ([[Sha256.digest]]): the JVM's, LÖVE's (native code) in the
  * client, a portable one elsewhere (the tests on Node.js).
  *
  * @param key
  *   any length (longer than a block, it is hashed first)
  */
final class Hmac(key: Array[Byte]) {
  import Hmac.*

  private val block: Array[Byte] = {
    val shortened = if key.length > BlockSize then Sha256.digest(key) else key
    val padded    = new Array[Byte](BlockSize)
    System.arraycopy(shortened, 0, padded, 0, shortened.length)
    padded
  }
  private val inner = block.map(b => (b ^ 0x36).toByte)
  private val outer = block.map(b => (b ^ 0x5c).toByte)

  /** The tag of the concatenation of the parts (32 bytes). */
  def apply(parts: Array[Byte]*): Array[Byte] =
    Sha256.digest(concat(Seq(outer, Sha256.digest(concat(inner +: parts)))))

  /** The tag, truncated to [[TagSize]] bytes: what goes in the datagrams. */
  def tag(parts: Array[Byte]*): Array[Byte] = apply(parts*).take(TagSize)

  /** Whether `tag` is the tag of the parts (in a time that does not depend on where they differ).
    */
  def verify(tag: Array[Byte], parts: Array[Byte]*): Boolean =
    Hmac.sameBytes(this.tag(parts*), tag)

  /** A key derived from this one, for another use (e.g., one direction of a session). */
  def derive(label: String, parts: Array[Byte]*): Hmac = Hmac(apply(utf8(label) +: parts*))
}

object Hmac {
  val BlockSize = 64

  /** 128 bits: plenty for tags that must be forged online, one datagram at a time. */
  val TagSize = 16

  def apply(key: Array[Byte]): Hmac = new Hmac(key)

  /** For the secrets that the menu server hands out. */
  def apply(secret: String): Hmac = new Hmac(utf8(secret))

  def utf8(s: String): Array[Byte] = s.getBytes(StandardCharsets.UTF_8)

  def concat(parts: Seq[Array[Byte]]): Array[Byte] = {
    val result = new Array[Byte](parts.iterator.map(_.length).sum)
    var offset = 0
    for part <- parts do {
      System.arraycopy(part, 0, result, offset, part.length)
      offset += part.length
    }
    result
  }

  /** Constant-time comparison. */
  def sameBytes(a: Array[Byte], b: Array[Byte]): Boolean =
    a.length == b.length && {
      var difference = 0
      var i          = 0
      while i < a.length do {
        difference |= a(i) ^ b(i)
        i += 1
      }
      difference == 0
    }

  /** The 8 bytes of `n`, big-endian. */
  def longBytes(n: Long): Array[Byte] = Array.tabulate[Byte](8)(i => (n >>> (56 - 8 * i)).toByte)

  def readLong(bytes: Array[Byte], offset: Int): Long = {
    var n = 0L
    var i = 0
    while i < 8 do {
      n = (n << 8) | (bytes(offset + i) & 0xffL)
      i += 1
    }
    n
  }
}

/** SHA-256 in plain Scala (FIPS 180-4), for the platforms that have nothing better. */
object PortableSha256 {
  private val K: Array[Int] = Array(
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
  )

  def digest(data: Array[Byte]): Array[Byte] = {
    val h = Array(0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c,
      0x1f83d9ab, 0x5be0cd19)
    // the data, 0x80, zeros, and its length in bits: a multiple of 64 bytes
    val length = ((data.length + 9 + 63) / 64) * 64
    val padded = new Array[Byte](length)
    System.arraycopy(data, 0, padded, 0, data.length)
    padded(data.length) = 0x80.toByte
    val bits = Hmac.longBytes(data.length.toLong * 8)
    System.arraycopy(bits, 0, padded, length - 8, 8)

    val w     = new Array[Int](64)
    var block = 0
    while block < length do {
      var t = 0
      while t < 16 do {
        val i = block + 4 * t
        w(t) = (padded(i) << 24) | ((padded(i + 1) & 0xff) << 16) | ((padded(i + 2) & 0xff) << 8) |
          (padded(i + 3) & 0xff)
        t += 1
      }
      while t < 64 do {
        val s0 = Integer.rotateRight(w(t - 15), 7) ^ Integer.rotateRight(w(t - 15), 18) ^
          (w(t - 15) >>> 3)
        val s1 = Integer.rotateRight(w(t - 2), 17) ^ Integer.rotateRight(w(t - 2), 19) ^
          (w(t - 2) >>> 10)
        w(t) = w(t - 16) + s0 + w(t - 7) + s1
        t += 1
      }
      var a = h(0); var b = h(1); var c = h(2); var d  = h(3)
      var e = h(4); var f = h(5); var g = h(6); var hh = h(7)
      t = 0
      while t < 64 do {
        val s1 = Integer.rotateRight(e, 6) ^ Integer.rotateRight(e, 11) ^ Integer.rotateRight(e, 25)
        val ch = (e & f) ^ (~e & g)
        val t1 = hh + s1 + ch + K(t) + w(t)
        val s0 = Integer.rotateRight(a, 2) ^ Integer.rotateRight(a, 13) ^ Integer.rotateRight(a, 22)
        val t2 = s0 + ((a & b) ^ (a & c) ^ (b & c))
        hh = g; g = f; f = e; e = d + t1; d = c; c = b; b = a; a = t1 + t2
        t += 1
      }
      h(0) += a; h(1) += b; h(2) += c; h(3) += d
      h(4) += e; h(5) += f; h(6) += g; h(7) += hh
      block += 64
    }
    Array.tabulate[Byte](32)(i => (h(i / 4) >>> (24 - 8 * (i % 4))).toByte)
  }
}
