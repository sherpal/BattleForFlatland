package services.localstorage

import java.time.{LocalDateTime, ZoneOffset}

import services.localstorage.LocalStorage.Key
import io.circe.{Decoder, Encoder, Json}

import scala.concurrent.duration.Duration

trait LocalStorage {

  private def now: LocalDateTime =
    LocalDateTime.ofEpochSecond(java.lang.System.currentTimeMillis() / 1000, 0, ZoneOffset.UTC)

  protected case class StoredItem[A](a: A, expireAt: LocalDateTime)

  protected given [A](using encoder: Encoder[A]): Encoder[StoredItem[A]] =
    Encoder.instance { storedItem =>
      Json.obj(
        "element"   -> encoder(storedItem.a),
        "timestamp" -> Encoder[LocalDateTime].apply(storedItem.expireAt)
      )
    }

  protected given [A](using decoder: Decoder[A]): Decoder[StoredItem[A]] =
    Decoder.instance { cursor =>
      for {
        a         <- decoder.tryDecode(cursor.downField("element"))
        timestamp <- Decoder[LocalDateTime].tryDecode(cursor.downField("timestamp"))
      } yield StoredItem(a, timestamp)
    }

  /** Stores the given `storedItem` at the `key` position using the concrete underlying storage
    * system.
    */
  protected def storeStoredItemAt[A](key: Key[A], storedItem: StoredItem[A])(using
      encoder: Encoder[A]
  ): Unit

  /** Retrieves the element from the underlying storage system at position `key` and tries to
    * interpret it as an element of type `A`.
    *
    * Throws an [[io.circe.Error]] if decoding failed.
    */
  protected def retrieveStoredItemFrom[A](key: Key[A])(using
      decoder: Decoder[A]
  ): Option[StoredItem[A]]

  /** Store the given element at the given key, to be retrieved later.
    * @param key
    *   place to store the element
    * @param element
    *   element to store
    * @param duration
    *   how much time this element should be considered as stored before being discarded.
    */
  final def storeAtFor[A](key: Key[A], element: A, duration: Duration)(using
      encoder: Encoder[A]
  ): Unit = {
    val expireAt =
      if (duration.isFinite) now.plusSeconds(duration.toSeconds)
      else LocalDateTime.MAX
    storeStoredItemAt(key, StoredItem(element, expireAt))
  }

  /** Try to retrieve an element of type A from the local storage. If the element is expired as
    * specified when it was first inserted, returns None.
    *
    * Throws an [[io.circe.Error]] if the element at place `key` was not of type `A`.
    *
    * @param key
    *   place to retrieve the element from
    * @tparam A
    *   type of element to decode
    */
  final def retrieveFrom[A](key: Key[A])(using
      decoder: Decoder[A]
  ): Option[A] =
    retrieveStoredItemFrom[A](key).filterNot(_.expireAt.isBefore(now)).map(_.a)

  def clearKey[T](key: Key[T]): Unit

}

object LocalStorage {

  /** Keys to store information in the local storage.
    *
    * Type parameter corresponds to the kind of object stored at that key
    */
  opaque type Key[T] = String
  def key[T](value: String): Key[T] = value

  object Key {
    extension [T](key: Key[T]) {
      def value: String = key
    }
  }
}
