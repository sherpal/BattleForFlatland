package programs.frontend.menus

import models.bff.ingame.{Controls, KeyboardControls}
import models.syntax.Pointed
import services.localstorage._
import zio.{UIO, ZIO}
import io.circe.generic.auto._
import services.logging.Logging

package object controls {

  /** Retrieve the [[Controls]] from the local storage if it exists, or the default one otherwise. */
  val retrieveControls: ZIO[Logging & LocalStorage, Nothing, Controls] = for {
    maybeFromLocalStorage <- retrieveFrom[Controls](KeyboardControls.storageKey)
      .catchAll(
        t =>
          services.logging.log.error(s"Failed to retrieve controls: ${t.getMessage}") *> ZIO
            .some(Pointed[Controls].unit)
      )
    keyboardControls = maybeFromLocalStorage.getOrElse(Pointed[Controls].unit)
  } yield keyboardControls

  /** Stores the given [[Controls]] and returns it. */
  def storeControls(controls: Controls): ZIO[LocalStorage, Throwable, Controls] =
    storeAt(KeyboardControls.storageKey, controls) *> UIO(controls)

  val resetControls = clearKey(Controls.storageKey)
}
