package models.bff.outofgame

import models.users.User

final case class MenuGameWithPlayers(game: MenuGame, players: List[User])
