package programs.frontend.menus.controls

import models.bff.ingame.{Controls, KeyboardControls}
import models.syntax.Pointed
import services.localstorage._
import zio.{UIO, ZIO}
import io.circe.generic.auto._
import services.logging.Logging

/** Retrieve the [[Controls]] from the local storage if it exists, or the default one otherwise. */
val retrieveControls: ZIO[Logging & LocalStorage, Nothing, Controls] = for {
  maybeFromLocalStorage <- KeyboardControls.storageKey.retrieve
    .catchAll(t =>
      services.logging.log.error(s"Failed to retrieve controls: ${t.getMessage}") *> ZIO
        .some(Pointed[Controls].unit)
    )
  keyboardControls = maybeFromLocalStorage.getOrElse(Pointed[Controls].unit)
} yield keyboardControls

/** Stores the given [[Controls]] and returns it. */
def storeControls(controls: Controls): ZIO[LocalStorage, Throwable, Controls] =
  KeyboardControls.storageKey.store(controls) *> ZIO.succeed(controls)

val resetControls = clearKey(Controls.storageKey)
