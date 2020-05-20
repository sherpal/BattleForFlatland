package services

import izumi.reflect.Tag
import zio.{Has, URIO, ZIO}

/**
  * Actions describe how frontend requests are handled by the backend.
  */
package object actions {

  type Action[A] = Has[Action.Service[A]]

  def body[A](implicit tagged: Tag[A]): URIO[Action[A], A] =
    ZIO.accessM(_.get[Action.Service[A]].body)

  def getFromSession[A](key: String)(implicit tagged: Tag[A]): URIO[Action[A], Option[String]] =
    ZIO.accessM(_.get[Action.Service[A]].getFromSession(key))

}
