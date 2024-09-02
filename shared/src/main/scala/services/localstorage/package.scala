package services.localstorage

import io.circe.{Decoder, Encoder}
import services.localstorage.LocalStorage.Key
import zio.{Task, ZIO}

import scala.concurrent.duration.Duration

def storeAt[A](key: Key[A], element: A)(using Encoder[A]): ZIO[LocalStorage, Throwable, Unit] =
  ZIO.serviceWithZIO[LocalStorage](_.storeAtFor(key, element, Duration.Inf))

def storeAtWithEffect[A](key: Key[A], element: A, effect: A => Task[Unit])(using
    encoder: Encoder[A]
): ZIO[LocalStorage, Throwable, Unit] =
  for {
    _ <- effect(element)
    _ <- key.store(element)
  } yield ()

def storeAtFor[A](key: Key[A], element: A, duration: Duration)(using
    encoder: Encoder[A]
): ZIO[LocalStorage, Throwable, Unit] =
  ZIO.serviceWithZIO[LocalStorage](_.storeAtFor(key, element, duration))

def retrieveFrom[A](key: Key[A])(using
    decoder: Decoder[A]
): ZIO[LocalStorage, Throwable, Option[A]] = ZIO.serviceWithZIO[LocalStorage](_.retrieveFrom(key))

def clearKey[T](key: Key[T]): ZIO[LocalStorage, Throwable, Unit] =
  ZIO.serviceWithZIO[LocalStorage](_.clearKey(key))
