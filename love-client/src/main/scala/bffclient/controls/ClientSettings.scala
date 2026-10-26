package bffclient.controls

import bffclient.love.Filesystem
import io.circe.Codec
import io.circe.parser.decode
import io.circe.syntax.*

/** The client's own settings (BFF's [[models.bff.ingame.Controls]] cannot hold them), stored as JSON
  * in LÖVE's save directory and changed with the controls.
  *
  * @param hudScale
  *   the size of the HUD (frames, action bar, ...), whatever the window's size
  */
final case class ClientSettings(hudScale: Double) {

  /** Writes the settings (used by the next game). An error message if it failed. */
  def save(): Option[String] = {
    val written = Filesystem.write(ClientSettings.fileName, this.asJson.spaces2)
    Option.unless(written.success)(
      s"Could not write ${Filesystem.getSaveDirectory()}/${ClientSettings.fileName}: ${written.message}"
    )
  }
}

object ClientSettings {
  val fileName = "settings.json"

  val default = ClientSettings(hudScale = 1.0)

  /** From 0.5 to 2. */
  val hudScales: Seq[Double] = (5 to 20).map(_ / 10.0)

  given Codec[ClientSettings] = io.circe.generic.semiauto.deriveCodec

  def load(): ClientSettings =
    if Filesystem.getInfo(fileName) == null then default
    else
      decode[ClientSettings](Filesystem.read(fileName).contents).toOption
        .filter(settings => hudScales.contains(settings.hudScale))
        .getOrElse(default)
}
