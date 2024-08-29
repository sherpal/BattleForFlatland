package services.localstorage

import services.localstorage.LocalStorage.Key
import zio.*
import io.circe.Decoder
import io.circe.Encoder
import java.nio.charset.StandardCharsets

class BLocalStorage(fileUpdateSemaphore: Semaphore, folder: os.Path) extends LocalStorage {

  override protected def storeStoredItemAt[A](key: Key, storedItem: StoredItem[A])(using
      encoder: Encoder[A]
  ): ZIO[Any, Throwable, Unit] = fileUpdateSemaphore.withPermit(ZIO.attemptBlockingIO {
    val filePath = keyToPath(key)
    if os.exists(filePath) then os.remove(filePath)

    os.write(filePath, Encoder[StoredItem[A]].apply(storedItem).spaces2)
  })

  override protected def retrieveStoredItemFrom[A](key: Key)(using
      decoder: Decoder[A]
  ): ZIO[Any, Throwable, Option[StoredItem[A]]] = fileUpdateSemaphore
    .withPermit(ZIO.attemptBlockingIO {
      val filePath = keyToPath(key)
      Option.when(os.exists(filePath)) {
        val rawContent = os.read(filePath)
        io.circe.parser.decode[StoredItem[A]](rawContent)
      } match {
        case None               => Right(None)
        case Some(Right(value)) => Right(Some(value))
        case Some(Left(err))    => Left(err)
      }
    })
    .absolve

  override def clearKey(key: Key): ZIO[Any, Throwable, Unit] = ???

  private def keyToPath(key: Key): os.Path = {
    val encodedKey =
      java.util.Base64.getEncoder.encodeToString(key.getBytes(StandardCharsets.UTF_8))
    folder / (encodedKey ++ ".json")
  }

}

object BLocalStorage {
  val live = ZLayer.fromZIO(for {
    _                   <- Console.printLine("Initializing LocalStorage service...")
    folder              <- ZIO.succeed(os.pwd / "target" / "db")
    _                   <- ZIO.attemptBlockingIO(os.makeDir.all(folder)).orDie
    fileUpdateSemaphore <- Semaphore.make(1L)
    bLocalStorage       <- ZIO.succeed(BLocalStorage(fileUpdateSemaphore, folder))
  } yield (bLocalStorage: LocalStorage))
}
