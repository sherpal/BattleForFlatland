package services.localstorage

import io.circe.{Decoder, Encoder}
import services.localstorage.LocalStorage.Key
import zio.{ZIO, ZLayer}
import zio.clock.Clock
import io.circe.parser.decode
import org.scalajs.dom

object FLocalStorage {

  val live: ZLayer[Clock, Nothing, LocalStorage] =
    ZLayer.fromService { (c: Clock.Service) =>
      new LocalStorage.Service {

        private class ElementAbsentException extends Exception

        protected def clock: ZLayer[Any, Nothing, Clock] = ZLayer.succeed(c)

        protected def storeStoredItemAt[A](key: Key, storedItem: StoredItem[A])(
            implicit encoder: Encoder[A]
        ): ZIO[Any, Throwable, Unit] = ZIO.effectTotal {
          dom.window.localStorage.setItem(key, Encoder[StoredItem[A]].apply(storedItem).noSpaces)
        }

        protected def retrieveStoredItemFrom[A](key: Key)(
            implicit decoder: Decoder[A]
        ): ZIO[Any, Throwable, Option[StoredItem[A]]] =
          (for {
            rawContent <- ZIO
              .fromOption {
                Option(dom.window.localStorage.getItem(key))
              }
              .orElseFail(new ElementAbsentException)
            element <- ZIO.fromEither(decode[StoredItem[A]](rawContent))
          } yield Some(element)).catchSome {
            case _: ElementAbsentException => ZIO.none
          }
      }
    }

}
