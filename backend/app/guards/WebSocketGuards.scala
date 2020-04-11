package guards

import errors.ErrorADT
import models.users.User
import play.api.mvc.RequestHeader
import services.config.Configuration
import utils.playzio.PlayZIO._
import zio.clock.Clock
import zio.{Has, ZIO}

object WebSocketGuards {

  def authenticated: ZIO[Clock with Configuration with Has[RequestHeader], ErrorADT, User] =
    for {
      header <- zioRequestHeader
      user <- Guards.userFromRequestHeader(header)
    } yield user

}
