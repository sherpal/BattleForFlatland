package services

import io.circe.{Decoder, Encoder}
import services.localstorage.LocalStorage.Key
import zio.{Has, ZIO}

import scala.concurrent.duration.Duration

package object localstorage {

  type LocalStorage = Has[LocalStorage.Service]

  def storeAt[A](key: Key, element: A)(implicit encoder: Encoder[A]): ZIO[LocalStorage, Throwable, Unit] =
    ZIO.accessM(_.get.storeAt(key, element))

  def retrieveFrom[A](key: Key)(implicit decoder: Decoder[A]): ZIO[LocalStorage, Throwable, Option[A]] =
    ZIO.accessM(_.get.retrieveFrom(key))

  def retrieveFromWithExpiry[A](key: Key, duration: Duration)(
      implicit decoder: Decoder[A]
  ): ZIO[LocalStorage, Throwable, Option[A]] = ZIO.accessM(_.get.retrieveFromWithExpiry(key, duration))

}
