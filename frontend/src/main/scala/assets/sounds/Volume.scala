package assets.sounds

import services.localstorage.LocalStorage.Key
import services.localstorage._
import io.circe.generic.auto._
import services.logging._

import zio._

final case class Volume(value: Double) extends AnyVal

object Volume {

  def apply(value: Double): Volume = new Volume(0.0 max (value min 1.0))

  val full: Volume = Volume(1)

  val storageKey: Key = "soundVolume"

  val loadStoredVolume: URIO[Logging with LocalStorage, Volume] = retrieveFrom[Volume](storageKey)
    .catchAll(error => log.error(Option(error.getMessage()).getOrElse("Unknown error")) *> UIO(Some(full)))
    .someOrElse(full)

  def storeVolume(volume: Volume): URIO[LocalStorage, Unit] = storeAt(storageKey, volume).either.unit

}
