package services

import zio.{Has, Tagged, URIO, ZIO}

/**
  * Actions describe how frontend requests are handled by the backend.
  */
package object actions {

  type Action[A] = Has[Action.Service[A]]

  def body[A](implicit tagged: Tagged[A]): URIO[Action[A], A] =
    ZIO.accessM(_.get[Action.Service[A]].body)

  def getFromSession(key: String): URIO[Action[_], Option[String]] =
    ZIO.accessM(_.get[Action.Service[_]].getFromSession(key))

}
