package guards

import models.bff.outofgame.MenuGameWithPlayers
import models.users.User
import play.api.mvc.{Request, WrappedRequest}

final case class JoinedGameRequest[A](
    gameInfo: MenuGameWithPlayers,
    user: User,
    request: Request[A]
) extends WrappedRequest[A](request)
