package services.actions

import models.users.User
import zio.UIO

object UserSessionAction {

  trait Service[A] extends Action.Service[A] {

    def user: UIO[User]

  }

}
