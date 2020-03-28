package services.actions

import zio.UIO

object Action {

  trait Service[A] {

    def body: UIO[A]

    def getFromSession(key: String): UIO[Option[String]]

  }

}
