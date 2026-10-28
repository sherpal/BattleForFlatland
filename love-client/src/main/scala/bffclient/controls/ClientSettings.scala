package bffclient.controls

import bffclient.love.Filesystem
import io.circe.{Decoder, Encoder}
import io.circe.parser.decode
import io.circe.syntax.*

/** The client's own settings (BFF's [[models.bff.ingame.Controls]] cannot hold them), stored as JSON
  * in LÖVE's save directory and changed in the settings (see [[SettingsEditor]]).
  *
  * @param hudScale
  *   the size of the HUD (frames, action bar, ...), whatever the window's size
  * @param hostGameServers
  *   whether the player's computer may run the game server of their games (see
  *   `hosting.HostingAgent`). Without it, the client downloads nothing to run them, e.g. for the
  *   players on a mobile network.
  */
final case class ClientSettings(hudScale: Double, hostGameServers: Boolean) {

  /** Writes the settings (the HUD scale is used by the next game). An error message if it failed. */
  def save(): Option[String] = {
    ClientSettings.known = Some(this)
    val written = Filesystem.write(ClientSettings.fileName, this.asJson.spaces2)
    Option.unless(written.success)(
      s"Could not write ${Filesystem.getSaveDirectory()}/${ClientSettings.fileName}: ${written.message}"
    )
  }
}

object ClientSettings {
  val fileName = "settings.json"

  val default = ClientSettings(hudScale = 1.0, hostGameServers = true)

  /** From 0.5 to 2. */
  val hudScales: Seq[Double] = (5 to 20).map(_ / 10.0)

  given Encoder[ClientSettings] = io.circe.generic.semiauto.deriveEncoder

  // the settings written before one of them existed have its default
  given Decoder[ClientSettings] = Decoder.instance(cursor =>
    for
      hudScale <- cursor.getOrElse[Double]("hudScale")(default.hudScale)
      hostGameServers <- cursor.getOrElse[Boolean]("hostGameServers")(default.hostGameServers)
    yield ClientSettings(hudScale, hostGameServers)
  )

  private var known = Option.empty[ClientSettings]

  /** The settings as they are now: the last ones saved, otherwise the ones of the file (read once). */
  def current: ClientSettings = known.getOrElse {
    val loaded = load()
    known = Some(loaded)
    loaded
  }

  def load(): ClientSettings =
    if Filesystem.getInfo(fileName) == null then default
    else
      decode[ClientSettings](Filesystem.read(fileName).contents).toOption
        .filter(settings => hudScales.contains(settings.hudScale))
        .getOrElse(default)
}
