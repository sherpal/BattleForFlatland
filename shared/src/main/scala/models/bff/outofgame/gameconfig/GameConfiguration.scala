package models.bff.outofgame.gameconfig

import models.syntax.Pointed
import io.circe.generic.auto._
import io.circe.syntax._

/**
  * The [[models.bff.outofgame.gameconfig.GameConfiguration]] gathers all information about the configuration of the
  * game that players are about to play.
  *
  * Among these configuration options, we'll have
  * - which class each player is going to use
  * - the boss that players want to face
  * - the colour every player chooses
  * - ...
  *
  * In the database, this information will simply be inserted as the JSON (or other encoder) so that we don't need
  * to model it using SQL tables. In the future, this could perhaps be handled using Elastic Search or MongoDB.
  *
  * @param playersInfo Map from player name to their information.
  */
final case class GameConfiguration(
    playersInfo: Map[String, PlayerInfo],
    maybeBossName: Option[String]
) {

  /** Gives back a new instance of the game configuration with the new player added. */
  def addPlayer(playerName: String): GameConfiguration = copy(
    playersInfo = playersInfo + (playerName -> Pointed[PlayerInfo].unit.copy(playerName = playerName))
  )

  /** Replaces the information of the given player with the newly provided ones. */
  def modifyPlayer(playerInfo: PlayerInfo): GameConfiguration =
    copy(playersInfo = playersInfo + (playerInfo.playerName -> playerInfo))

  /** Removes the given player from the configuration */
  def removePlayer(playerName: String): GameConfiguration = copy(
    playersInfo = playersInfo - playerName
  )

  def withBossName(bossName: String): GameConfiguration = copy(maybeBossName = Some(bossName))

  def json: String = this.asJson.noSpaces

}
