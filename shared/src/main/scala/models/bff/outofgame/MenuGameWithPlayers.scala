package models.bff.outofgame

import menus.data.User
import io.circe.Codec
import models.bff.outofgame.gameconfig.PlayerInfo

final case class MenuGameWithPlayers(game: MenuGame, players: Vector[User]) {

  def id: String = game.gameId

  def forgetPassword: MenuGameWithPlayers = copy(game = game.forgetPassword)

  def onlyPlayerNames: MenuGameWithPlayers =
    MenuGameWithPlayers(game.onlyCreatorName, players)

  def containsPlayer(player: User): Boolean = players.contains[User](player)

  def withPlayer(playerInfo: PlayerInfo): MenuGameWithPlayers =
    copy(game = game.withPlayer(playerInfo))

  def removePlayer(playerName: String): MenuGameWithPlayers =
    copy(game = game.removePlayer(playerName), players = players.filterNot(_.name == playerName))

}

object MenuGameWithPlayers {

  given Codec[MenuGameWithPlayers] = io.circe.generic.semiauto.deriveCodec

}
