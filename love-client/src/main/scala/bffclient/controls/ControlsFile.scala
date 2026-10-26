package bffclient.controls

import bffclient.love.Filesystem
import io.circe.Encoder
import io.circe.parser.parse
import models.bff.ingame.Controls
import models.syntax.Pointed

/** The player's controls, stored as JSON in LÖVE's save directory.
  *
  * The file can contain either the JSON of a [[Controls]], or what the browser frontend stores in
  * its local storage under the key `controls` (`{"element": <controls>, "timestamp": ...}`).
  */
object ControlsFile {
  val fileName = "controls.json"

  /** Writes the controls (used by the next game). An error message if it failed. */
  def save(controls: Controls): Option[String] = {
    val written = Filesystem.write(fileName, Encoder[Controls].apply(controls).spaces2)
    Option.unless(written.success)(
      s"Could not write ${Filesystem.getSaveDirectory()}/$fileName: ${written.message}"
    )
  }

  /** The controls to use, and a message saying where they come from. */
  def load(): (Controls, String) = {
    val default = Pointed[Controls].unit
    val path    = s"${Filesystem.getSaveDirectory()}/$fileName"
    if Filesystem.getInfo(fileName) == null then {
      val written = Filesystem.write(fileName, Encoder[Controls].apply(default).spaces2)
      if written.success then (default, s"Default controls (written to $path)")
      else (default, s"Default controls (could not write $path: ${written.message})")
    } else {
      val decoded = for
        json     <- parse(Filesystem.read(fileName).contents)
        controls <- json.as[Controls].orElse(json.hcursor.downField("element").as[Controls])
      yield controls
      decoded match {
        case Right(controls) =>
          val duplicate =
            controls.maybeMultipleKey.fold("")(key => s" (warning: ${key.label} is assigned twice)")
          (controls, s"Controls loaded from $path$duplicate")
        case Left(error) =>
          (default, s"Default controls ($path is invalid: ${error.getMessage})")
      }
    }
  }
}
