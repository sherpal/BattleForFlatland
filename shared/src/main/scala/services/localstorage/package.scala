package services.localstorage

import io.circe.{Decoder, Encoder}
import services.localstorage.LocalStorage.Key
import zio.{Task, ZIO}

import scala.concurrent.duration.Duration

def storeAt[A](key: Key, element: A)(using encoder: Encoder[A]): ZIO[LocalStorage, Throwable, Unit] =
  ZIO.serviceWithZIO[LocalStorage](_.storeAtFor(key, element, Duration.Inf))

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
  ZIO.serviceWithZIO[LocalStorage](_.storeAtFor(key, element, duration))

def retrieveFrom[A](key: Key)(using
    decoder: Decoder[A]
): ZIO[LocalStorage, Throwable, Option[A]] = ZIO.serviceWithZIO[LocalStorage](_.retrieveFrom(key))

def clearKey(key: Key): ZIO[LocalStorage, Throwable, Unit] = ZIO.serviceWithZIO[LocalStorage](_.clearKey(key))
