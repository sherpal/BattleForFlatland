package services

import io.circe.{Decoder, Encoder}
import services.localstorage.LocalStorage.Key
import zio.{Has, Task, ZIO}

import scala.concurrent.duration.Duration

package object localstorage {

  type LocalStorage = Has[LocalStorage.Service]

  def storeAt[A](key: Key, element: A)(implicit encoder: Encoder[A]): ZIO[LocalStorage, Throwable, Unit] =
    ZIO.accessM(_.get.storeAtFor(key, element, Duration.Inf))

  def storeAtWithEffect[A](key: Key, element: A, effect: A => Task[Unit])(
      implicit encoder: Encoder[A]
  ): ZIO[LocalStorage, Throwable, Unit] =
    for {
      _ <- effect(element)
      _ <- storeAt(key, element)
    } yield ()

  def storeAtFor[A](key: Key, element: A, duration: Duration)(
      implicit encoder: Encoder[A]
  ): ZIO[LocalStorage, Throwable, Unit] =
    ZIO.accessM(_.get.storeAtFor(key, element, duration))

  def retrieveFrom[A](key: Key)(
      implicit decoder: Decoder[A]
  ): ZIO[LocalStorage, Throwable, Option[A]] = ZIO.accessM(_.get.retrieveFrom(key))

}
