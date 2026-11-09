package bffclient.render

import bffclient.love.{Audio, Filesystem, Love, Source}
import gamelogic.abilities.Ability

/** The sounds of the abilities: the files `assets/sounds/<name>.ogg`. An ability without its file
  * plays nothing. None without the audio module (headless).
  */
object Sounds {

  /** By ability (the square's, for now), when there is a file for it. */
  lazy val abilities: Map[Ability.AbilityId, Source] =
    if !Love.hasAudio then Map.empty
    else
      Map(
        Ability.square.hammerHitId  -> "hammer-hit",
        Ability.square.tauntId      -> "taunt",
        Ability.square.enrageId     -> "enrage",
        Ability.square.cleaveId     -> "cleave",
        Ability.square.braceId      -> "brace",
        Ability.square.shieldSlamId -> "shield-slam",
        Ability.square.guardId      -> "guard"
      ).flatMap((ability, name) => load(name).map(ability -> _))

  /** Plays the sound, even if it is already playing, at `volume` (between 0 and 1). */
  def play(source: Source, volume: Double): Unit = {
    val copy = source.cloned()
    copy.setVolume(volume)
    copy.play()
  }

  private def load(name: String): Option[Source] = {
    val path = s"assets/sounds/$name.ogg"
    Filesystem.getInfo(path).toOption.map(_ => Audio.newSource(path, "static"))
  }
}
