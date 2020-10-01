package services.localstorage

import java.time.{LocalDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit

import io.circe.{Decoder, Encoder, Json}
import zio.clock.Clock
import zio.{ZIO, ZLayer}

import scala.concurrent.duration.Duration

object LocalStorage {

  type Key = String

  trait Service {

    private def nowLocalDateTimeFromSeconds(epochSeconds: Long): LocalDateTime =
      LocalDateTime.ofEpochSecond(epochSeconds, 0, ZoneOffset.UTC)

    protected case class StoredItem[A](a: A, expireAt: LocalDateTime)

    protected implicit def storedItemEncoder[A](implicit encoder: Encoder[A]): Encoder[StoredItem[A]] =
      Encoder.instance { storedItem =>
        Json.obj(
          "element" -> encoder(storedItem.a),
          "timestamp" -> Encoder[LocalDateTime].apply(storedItem.expireAt)
        )
      }

    protected implicit def storedItemDecoder[A](implicit decoder: Decoder[A]): Decoder[StoredItem[A]] =
      Decoder.instance { cursor =>
        for {
          a         <- decoder.tryDecode(cursor.downField("element"))
          timestamp <- Decoder[LocalDateTime].tryDecode(cursor.downField("timestamp"))
        } yield StoredItem(a, timestamp)
      }

    protected def clock: ZLayer[Any, Nothing, Clock]

    /**
      * Stores the given `storedItem` at the `key` position using the concrete underlying storage system.
      */
    protected def storeStoredItemAt[A](key: Key, storedItem: StoredItem[A])(
        implicit encoder: Encoder[A]
    ): ZIO[Any, Throwable, Unit]

    /**
      * Retrieves the element from the underlying storage system at position `key` and tries to interpret it as an
      * element of type `A`.
      *
      * Fails with an [[io.circe.Error]] if decoding failed.
      */
    protected def retrieveStoredItemFrom[A](key: Key)(
        implicit decoder: Decoder[A]
    ): ZIO[Any, Throwable, Option[StoredItem[A]]]

    /**
      * Store the given element at the given key, to be retrieved later.
      * @param key place to store the element
      * @param element element to store
      * @param duration how much time this element should be considered as stored before being discarded.
      */
    final def storeAtFor[A](key: Key, element: A, duration: Duration)(
        implicit encoder: Encoder[A]
    ): ZIO[Any, Throwable, Unit] =
      for {
        now <- zio.clock.currentTime(TimeUnit.SECONDS).provideLayer(clock)
        dateTime = if (duration.isFinite) nowLocalDateTimeFromSeconds(now).plusSeconds(duration.toSeconds)
        else LocalDateTime.MAX
        storedItem = StoredItem(element, dateTime)
        _ <- storeStoredItemAt(key, storedItem)
      } yield ()

    /**
      * Try to retrieve an element of type A from the local storage.
      * If the element is expired as specified when it was first inserted, returns None.
      *
      * Fails with an [[io.circe.Error]] if the element at place `key` was not of type `A`.
      *
      * @param key place to retrieve the element from
      * @tparam A type of element to decode
      */
    final def retrieveFrom[A](key: Key)(
        implicit decoder: Decoder[A]
    ): ZIO[Any, Throwable, Option[A]] =
      for {
        maybeStoredElement <- retrieveStoredItemFrom[A](key)
        now                <- zio.clock.currentTime(TimeUnit.SECONDS).provideLayer(clock)
        maybeIsExpired = maybeStoredElement
          .map(_.expireAt)
          .map(_ isBefore nowLocalDateTimeFromSeconds(now))
        maybeElement = for {
          isExpired <- maybeIsExpired
          if !isExpired
          storedElement <- maybeStoredElement
        } yield storedElement.a
      } yield maybeElement

    def clearKey(key: Key): ZIO[Any, Throwable, Unit]

  }

}
