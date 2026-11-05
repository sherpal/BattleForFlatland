package bffclient.controls

import bffclient.love.Filesystem
import io.circe.{Decoder, Encoder}
import io.circe.parser.decode
import io.circe.syntax.*

/** The client's own settings (BFF's [[models.bff.ingame.Controls]] cannot hold them), stored as
  * JSON in LÖVE's save directory and changed in the settings (see [[SettingsEditor]]).
  *
  * @param hudScale
  *   the size of the HUD (frames, action bar, ...), whatever the window's size
  * @param hostGameServers
  *   whether the player's computer may run the game server of their games (see
  *   `hosting.HostingAgent`). Without it, the client downloads nothing to run them, e.g. for the
  *   players on a mobile network.
  * @param volumes
  *   how loud the sounds are
  */
final case class ClientSettings(hudScale: Double, hostGameServers: Boolean, volumes: Volumes) {

  /** Writes the settings (the HUD scale is used by the next game). An error message if it failed.
    */
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

  val default = ClientSettings(hudScale = 1.0, hostGameServers = true, volumes = Volumes.default)

  /** From 0.5 to 2. */
  val hudScales: Seq[Double] = (5 to 20).map(_ / 10.0)

  given Encoder[ClientSettings] = io.circe.generic.semiauto.deriveEncoder

  // the settings written before one of them existed have its default
  given Decoder[ClientSettings] = Decoder.instance(cursor =>
    for
      hudScale        <- cursor.getOrElse[Double]("hudScale")(default.hudScale)
      hostGameServers <- cursor.getOrElse[Boolean]("hostGameServers")(default.hostGameServers)
      volumes         <- cursor.getOrElse[Volumes]("volumes")(default.volumes)
    yield ClientSettings(hudScale, hostGameServers, volumes)
  )

  private var known = Option.empty[ClientSettings]

  /** The settings as they are now: the last ones saved, otherwise the ones of the file (read once).
    */
  def current: ClientSettings = known.getOrElse {
    val loaded = load()
    known = Some(loaded)
    loaded
  }

  def load(): ClientSettings =
    Filesystem.getInfo(fileName).fold(default) { _ =>
      decode[ClientSettings](Filesystem.read(fileName).contents).toOption
        .filter(settings => hudScales.contains(settings.hudScale) && settings.volumes.valid)
        .getOrElse(default)
    }
}

/** The volumes of the sounds, each a level from 0 (off) to 10 (full): the main one, and one per
  * kind of sound (only the abilities' for now), on top of the main one.
  */
final case class Volumes(main: Int, abilities: Int) {

  /** Of the sounds of the abilities, between 0 and 1. */
  def ofAbilities: Double = Volumes.fraction(main) * Volumes.fraction(abilities)

  def valid: Boolean = Seq(main, abilities).forall(Volumes.levels.contains)
}

object Volumes {
  val levels: Seq[Int] = 0 to 10

  val default = Volumes(main = 10, abilities = 10)

  private def fraction(level: Int): Double = level / levels.max.toDouble

  given Encoder[Volumes] = io.circe.generic.semiauto.deriveEncoder

  // like the settings, a volume added later has its default
  given Decoder[Volumes] = Decoder.instance(cursor =>
    for
      main      <- cursor.getOrElse[Int]("main")(default.main)
      abilities <- cursor.getOrElse[Int]("abilities")(default.abilities)
    yield Volumes(main, abilities)
  )
}
