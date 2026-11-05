package bffclient.tutorial

import bffclient.ui.Colour
import models.bff.ingame.Controls
import models.bff.outofgame.PlayerClasses
import services.i18n.{I18n, TextSection}

/** A page of the tutorial: how the game works, then one per class, with its role and the colour
  * that the tutorial gives it (players choose their own colour in the game). Its texts are the
  * language's (see [[I18n]]).
  */
enum Topic(val colour: Colour, val playerClass: Option[PlayerClasses]) {
  case Overview extends Topic(Colour.rgb(235, 190, 70), None)
  case Triangle extends Topic(Colour.rgb(230, 90, 70), Some(PlayerClasses.Triangle))
  case Square extends Topic(Colour.rgb(80, 140, 230), Some(PlayerClasses.Square))
  case Pentagon extends Topic(Colour.rgb(185, 110, 220), Some(PlayerClasses.Pentagon))
  case Hexagon extends Topic(Colour.rgb(70, 190, 110), Some(PlayerClasses.Hexagon))

  /** E.g. "The Triangle". */
  def title(i18n: I18n): String = playerClass.fold(i18n.overview)(i18n.theClass)

  /** The title in short, for the tabs: e.g. "Triangle". */
  def shortTitle(i18n: I18n): String = playerClass.fold(i18n.overview)(i18n.playerClassName)

  def role(i18n: I18n): String    = playerClass.fold(i18n.overviewRole)(i18n.classRole)
  def summary(i18n: I18n): String = playerClass.fold(i18n.overviewSummary)(i18n.classSummary)

  /** What the page says, with the player's keys (from their controls). */
  def sections(i18n: I18n, controls: Controls): Seq[TextSection] =
    playerClass.fold(i18n.overviewSections(controls))(i18n.classSections)

  def previous: Option[Topic] = Topic.values.lift(ordinal - 1)
  def next: Option[Topic]     = Topic.values.lift(ordinal + 1)
}

object Topic {
  def of(playerClass: PlayerClasses): Topic = values.find(_.playerClass.contains(playerClass)).get
}
