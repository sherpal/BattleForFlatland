package guards

import models.User
import play.api.mvc.{Request, WrappedRequest}

final case class SessionRequest[A](user: User, request: Request[A]) extends WrappedRequest[A](request)
