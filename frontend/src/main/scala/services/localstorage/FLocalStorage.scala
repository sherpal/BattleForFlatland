package services.localstorage

import io.circe.parser.decode
import io.circe.{Decoder, Encoder}
import org.scalajs.dom
import services.localstorage.LocalStorage.Key
import zio.*

class FLocalStorage extends LocalStorage {

  private class ElementAbsentException extends Exception

  protected def storeStoredItemAt[A](key: Key[A], storedItem: StoredItem[A])(implicit
      encoder: Encoder[A]
  ): ZIO[Any, Throwable, Unit] = ZIO.succeed {
    dom.window.localStorage.setItem(key.value, Encoder[StoredItem[A]].apply(storedItem).noSpaces)
  }

  protected def retrieveStoredItemFrom[A](
      key: Key[A]
  )(using Decoder[A]): ZIO[Any, Throwable, Option[StoredItem[A]]] =
    (for {
      rawContent <- ZIO
        .fromOption {
          Option(dom.window.localStorage.getItem(key.value))
        }
        .orElseFail(new ElementAbsentException)
      element <- ZIO.fromEither(decode[StoredItem[A]](rawContent))
    } yield Some(element)).catchSome { case _: ElementAbsentException =>
      ZIO.none
    }

  def clearKey[A](key: Key[A]): ZIO[Any, Throwable, Unit] =
    ZIO.succeed(dom.window.localStorage.removeItem(key.value))
}

object FLocalStorage {

  val live = ZLayer.fromZIO(for {
    _             <- Console.printLine("Initializing LocalStorage service...")
    fLocalStorage <- ZIO.succeed(FLocalStorage())
  } yield (fLocalStorage: LocalStorage))

}
