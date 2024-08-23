package services

import io.circe.{Decoder, Encoder}
import services.localstorage.LocalStorage.Key
import zio.{Has, Task, ZIO}

import scala.concurrent.duration.Duration

package object localstorage {

  type LocalStorage = Has[LocalStorage.Service]

  def storeAt[A](key: Key, element: A)(using encoder: Encoder[A]): ZIO[LocalStorage, Throwable, Unit] =
    ZIO.accessM(_.get.storeAtFor(key, element, Duration.Inf))

  def storeAtWithEffect[A](key: Key, element: A, effect: A => Task[Unit])(using
      encoder: Encoder[A]
  ): ZIO[LocalStorage, Throwable, Unit] =
    for {
      _ <- effect(element)
      _ <- storeAt(key, element)
    } yield ()

  def storeAtFor[A](key: Key, element: A, duration: Duration)(using
      encoder: Encoder[A]
  ): ZIO[LocalStorage, Throwable, Unit] =
    ZIO.accessM(_.get.storeAtFor(key, element, duration))

  def retrieveFrom[A](key: Key)(using
      decoder: Decoder[A]
  ): ZIO[LocalStorage, Throwable, Option[A]] = ZIO.accessM(_.get.retrieveFrom(key))

  def clearKey(key: Key): ZIO[LocalStorage, Throwable, Unit] = ZIO.accessM(_.get.clearKey(key))

}
