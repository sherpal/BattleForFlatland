package models.bff.outofgame

import models.syntax.Pointed
import models.users.User

final case class MenuGameWithPlayers(game: MenuGame, players: List[User]) {

  def onlyPlayerNames: MenuGameWithPlayers = MenuGameWithPlayers(game.onlyCreatorName, players.map(_.onlyName))

}

object MenuGameWithPlayers {

  implicit def pointed(implicit gamePointed: Pointed[MenuGame]): Pointed[MenuGameWithPlayers] = Pointed.factory(
    MenuGameWithPlayers(gamePointed.unit, Nil)
  )

}
