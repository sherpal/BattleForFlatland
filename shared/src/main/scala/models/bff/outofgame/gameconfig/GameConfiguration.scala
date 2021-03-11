package models.bff.outofgame.gameconfig

import io.circe.generic.auto._
import io.circe.syntax._
import models.bff.outofgame.gameconfig.GameConfiguration.ValidGameConfiguration
import models.bff.outofgame.gameconfig.PlayerInfo.ValidPlayerInfo
import models.syntax.Pointed
import gamelogic.docs.BossMetadata

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
    maybeBossName: Option[String] // todo: list of boss names instead?
) {

  /** Gives back a new instance of the game configuration with the new player added. */
  def addPlayer(playerName: PlayerName): GameConfiguration = copy(
    playersInfo = playersInfo + (playerName.name -> Pointed[PlayerInfo].unit.copy(playerName = playerName))
  )

  /** Replaces the information of the given player with the newly provided ones. */
  def modifyPlayer(playerInfo: PlayerInfo): GameConfiguration =
    copy(playersInfo = playersInfo + (playerInfo.playerName.name -> playerInfo))

  /** Removes the given player from the configuration */
  def removePlayer(playerName: PlayerName): GameConfiguration = copy(
    playersInfo = playersInfo - playerName.name
  )

  def withBossName(bossName: String): GameConfiguration = copy(maybeBossName = Some(bossName))

  /**
    * Removes all current [[PlayerType.ArtificialIntelligence]], moves all
    * [[PlayerType.Human]] to [[PlayerType.Observer]] and create a [[PlayerType.ArtificialIntelligence]]
    * for each class in the [[BossMetadata]] description
    */
  def aisOnly(bossName: String): GameConfiguration =
    (for {
      metadata      <- BossMetadata.maybeMetadataByName(bossName)
      aiComposition <- metadata.maybeAICompositionWithNames
      newPlayers = playersInfo
        .filter(_._2.playerType != PlayerType.ArtificialIntelligence)
        .map { case (name, info) => (name, info.copy(playerType = PlayerType.Observer)) }
    } yield newPlayers ++ aiComposition.toMap).fold(this)(newPlayers => copy(playersInfo = newPlayers))

  /**
    * Removes [[PlayerType.ArtificialIntelligence]] and restore all [[PlayerType.Observer]]
    * as [[PlayerType.Human]].
    */
  def removeAis: GameConfiguration =
    copy(playersInfo = playersInfo.collect {
      case (name, info) if info.playerType != PlayerType.ArtificialIntelligence =>
        (name, info.copy(playerType = PlayerType.Human))
    })

  def toggleAis(withAI: Boolean): GameConfiguration = if (withAI) maybeBossName.fold(this)(aisOnly) else removeAis

  def isValid: Boolean = asValid.isDefined

  def asValid: Option[ValidGameConfiguration] =
    for {
      bossName <- maybeBossName
      validPlayersInfo = playersInfo
        .map { case (name, info) => name -> info.asValid }
        .collect { case (name, Some(info)) => name -> info }
      if validPlayersInfo.size == playersInfo.size
    } yield ValidGameConfiguration(validPlayersInfo, bossName)

  def json: String = this.asJson.noSpaces

}

object GameConfiguration {

  final case class ValidGameConfiguration(
      playersInfo: Map[String, ValidPlayerInfo],
      bossName: String
  ) {
    def json: String = this.asJson.noSpaces
  }

}
