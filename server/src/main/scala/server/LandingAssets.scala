package server

import java.security.MessageDigest

import services.i18n.LandingPicture

/** The landing page's pictures and animation (the `landing/` resources), read once, and served by
  * [[LandingRoutes]] at `/landing/<hash>/<name>`: the hash of their content is in their address, so
  * that browsers keep them for good, and see a new one at its new address.
  */
object LandingAssets {

  /** A file, with its type, and the hash of its content (which is its ETag). */
  final class Asset(val name: String, val bytes: Array[Byte], val contentType: String) {
    val hash: String = LandingAssets.hash(bytes)
    def url: String  = s"/landing/$hash/$name"
  }

  def pictureName(picture: LandingPicture): String = s"${picture.toString.toLowerCase}.png"

  val video = "battle.mp4"
  val gif   = "battle.gif"

  /** The asset of this name, if the server has it. */
  def apply(name: String): Option[Asset] = assets.get(name)

  private lazy val assets: Map[String, Asset] =
    (LandingPicture.values.toSeq.map(pictureName) ++ Seq(video, gif)).flatMap { name =>
      Option(getClass.getResourceAsStream(s"/landing/$name")).map { stream =>
        val bytes =
          try stream.readAllBytes()
          finally stream.close()
        name -> Asset(name, bytes, contentType(name))
      }
    }.toMap

  private def contentType(name: String): String = name.split('.').last match {
    case "png" => "image/png"
    case "gif" => "image/gif"
    case "mp4" => "video/mp4"
    case _ => "application/octet-stream"
  }

  /** The beginning of the SHA-256 of these bytes, in hexadecimal: enough to tell versions apart. */
  def hash(bytes: Array[Byte]): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).take(8).map("%02x".format(_)).mkString
}
