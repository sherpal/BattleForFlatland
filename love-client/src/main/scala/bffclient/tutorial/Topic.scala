package bffclient.tutorial

import bffclient.ui.Colour
import models.bff.outofgame.PlayerClasses

/** A page of the tutorial: how the game works, then one per class, with its role and the colour
  * that the tutorial gives it (players choose their own colour in the game).
  */
enum Topic(
    val title: String,
    val role: String,
    val summary: String,
    val colour: Colour,
    val playerClass: Option[PlayerClasses]
) {
  case Overview
      extends Topic(
        "Overview",
        "How the game works",
        "A team of shapes against a boss: who does what, how to read the screen, and how a fight goes.",
        Colour.rgb(235, 190, 70),
        None
      )
  case Triangle
      extends Topic(
        "The Triangle",
        "Melee damage",
        "Fights in contact with its enemies, spends its energy on big hits, and controls the adds.",
        Colour.rgb(230, 90, 70),
        Some(PlayerClasses.Triangle)
      )
  case Square
      extends Topic(
        "The Square",
        "Tank",
        "Keeps the boss' attention and takes its blows, behind a shield it keeps building up.",
        Colour.rgb(80, 140, 230),
        Some(PlayerClasses.Square)
      )
  case Pentagon
      extends Topic(
        "The Pentagon",
        "Ranged damage",
        "Shoots its enemies from afar, aims its bullets and zones, and dispels nasty debuffs.",
        Colour.rgb(185, 110, 220),
        Some(PlayerClasses.Pentagon)
      )
  case Hexagon
      extends Topic(
        "The Hexagon",
        "Healer",
        "Keeps everyone alive, with a big but slowly regenerating pool of mana to manage.",
        Colour.rgb(70, 190, 110),
        Some(PlayerClasses.Hexagon)
      )

  def previous: Option[Topic] = Topic.values.lift(ordinal - 1)
  def next: Option[Topic]     = Topic.values.lift(ordinal + 1)
}

object Topic {
  def of(playerClass: PlayerClasses): Topic = values.find(_.playerClass.contains(playerClass)).get
}
