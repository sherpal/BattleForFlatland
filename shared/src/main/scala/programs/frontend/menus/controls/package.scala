package programs.frontend.menus

import models.bff.ingame.KeyboardControls
import models.syntax.Pointed
import services.localstorage._
import zio.{UIO, ZIO}
import io.circe.generic.auto._

package object controls {

  /** Retrieve the [[KeyboardControls]] from the local storage if it exists, or the default one otherwise. */
  val retrieveKeyboardControls: ZIO[LocalStorage, Nothing, KeyboardControls] = for {
    maybeFromLocalStorage <- retrieveFrom[KeyboardControls](KeyboardControls.storageKey)
      .catchAll(_ => ZIO.some(Pointed[KeyboardControls].unit))
    keyboardControls = maybeFromLocalStorage.getOrElse(Pointed[KeyboardControls].unit)
  } yield keyboardControls

  /** Stores the given [[KeyboardControls]] and returns it. */
  def storeKeyboardControls(keyboardControls: KeyboardControls): ZIO[LocalStorage, Throwable, KeyboardControls] =
    storeAt(KeyboardControls.storageKey, keyboardControls) *> UIO(keyboardControls)

  val resetKeyboardControls = clearKey(KeyboardControls.storageKey)
}
