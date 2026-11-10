package services.localstorage

import services.localstorage.LocalStorage.Key
import io.circe.Decoder
import io.circe.Encoder
import java.nio.charset.StandardCharsets

/** Stores each key in its own json file, in `folder`. */
class BLocalStorage(folder: os.Path) extends LocalStorage {

  os.makeDir.all(folder)

  private val fileUpdateLock = new Object

  override protected def storeStoredItemAt[A](key: Key[A], storedItem: StoredItem[A])(using
      encoder: Encoder[A]
  ): Unit = fileUpdateLock.synchronized {
    val filePath = keyToPath(key)
    if os.exists(filePath) then os.remove(filePath)

    os.write(filePath, Encoder[StoredItem[A]].apply(storedItem).spaces2)
  }

  override protected def retrieveStoredItemFrom[A](key: Key[A])(using
      decoder: Decoder[A]
  ): Option[StoredItem[A]] = fileUpdateLock.synchronized {
    val filePath = keyToPath(key)
    Option.when(os.exists(filePath)) {
      io.circe.parser.decode[StoredItem[A]](os.read(filePath)).toTry.get
    }
  }

  override def clearKey[T](key: Key[T]): Unit = fileUpdateLock.synchronized {
    os.remove(keyToPath(key), checkExists = false)
  }

  private def keyToPath[T](key: Key[T]): os.Path = {
    val encodedKey =
      java.util.Base64.getEncoder.encodeToString(key.value.getBytes(StandardCharsets.UTF_8))
    folder / (encodedKey ++ ".json")
  }

}

object BLocalStorage {
  def live(): BLocalStorage = {
    println("Initializing LocalStorage service...")
    BLocalStorage(os.pwd / "target" / "db")
  }
}
