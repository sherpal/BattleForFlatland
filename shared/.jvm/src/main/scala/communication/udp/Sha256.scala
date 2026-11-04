package communication.udp

import java.security.MessageDigest

/** SHA-256, the JVM's. */
object Sha256 {
  def digest(data: Array[Byte]): Array[Byte] = MessageDigest.getInstance("SHA-256").digest(data)
}
