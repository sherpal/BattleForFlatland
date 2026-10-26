package bffclient.menu

import bffclient.love.Filesystem
import io.circe.Codec
import io.circe.parser.decode
import io.circe.syntax.*
import models.bff.outofgame.PlayerClasses
import utils.misc.RGBColour

/** What the menus remember between runs, in LÖVE's save directory (the frontend keeps the same
  * things in its local storage).
  */
final case class MenuPreferences(
    /** The menu server's address, as typed on the login screen. */
    server: Option[String],
    userName: String,
    playerClass: Option[PlayerClasses],
    colour: Option[RGBColour],
    bossName: Option[String]
) {
  def save(): Unit = {
    val written = Filesystem.write(MenuPreferences.fileName, this.asJson.spaces2)
    if !written.success then println(s"Could not save the menu preferences: ${written.message}")
  }
}

object MenuPreferences {
  val fileName = "menu.json"

  given Codec[MenuPreferences] = io.circe.generic.semiauto.deriveCodec

  def load(): MenuPreferences = {
    val empty = MenuPreferences(None, "", None, None, None)
    if Filesystem.getInfo(fileName) == null then empty
    else decode[MenuPreferences](Filesystem.read(fileName).contents).getOrElse(empty)
  }
}
