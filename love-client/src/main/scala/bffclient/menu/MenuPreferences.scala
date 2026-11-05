package bffclient.menu

import bffclient.love.Filesystem
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
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
    bossName: Option[String],
    /** Whether "Learn to play" was ever opened (it is highlighted until then). */
    tutorialVisited: Boolean
) {
  def save(): Unit = {
    val written = Filesystem.write(MenuPreferences.fileName, this.asJson.spaces2)
    if !written.success then println(s"Could not save the menu preferences: ${written.message}")
  }
}

object MenuPreferences {
  val fileName = "menu.json"

  given Encoder[MenuPreferences] = deriveEncoder
  // preferences saved before `tutorialVisited` existed must still decode, as not visited
  given Decoder[MenuPreferences] = deriveDecoder[MenuPreferences].prepare(
    _.withFocus(
      _.mapObject(obj =>
        if obj.contains("tutorialVisited") then obj else obj.add("tutorialVisited", Json.False)
      )
    )
  )

  def load(): MenuPreferences = {
    val empty = MenuPreferences(None, "", None, None, None, tutorialVisited = false)
    Filesystem
      .getInfo(fileName)
      .fold(empty)(_ =>
        decode[MenuPreferences](Filesystem.read(fileName).contents).getOrElse(empty)
      )
  }
}
