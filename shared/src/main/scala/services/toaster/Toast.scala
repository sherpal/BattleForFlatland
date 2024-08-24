package services.toaster

import zio.{URIO, ZIO}

final class Toast {

  /** Toast an info with the given modifiers.
    *
    * @example
    * {{{
    *   import services.toaster.ToasterModifierBuilder._
    *   toast.info("Logged in!", autoCloseDuration := 2.seconds)
    * }}}
    */
  def info(content: String, modifiers: ToasterModifier*): URIO[Toaster, Unit] =
    ZIO.serviceWithZIO[Toaster](_.info(content, ToasterModifier.seqBuild(modifiers)))

  /** Toast an success with the given modifiers.
    *
    * @example
    * {{{
    *   import services.toaster.ToasterModifierBuilder._
    *   toast.success("Logged in!", autoCloseDuration := 2.seconds)
    * }}}
    */
  def success(content: String, modifiers: ToasterModifier*): URIO[Toaster, Unit] =
    ZIO.serviceWithZIO[Toaster](_.success(content, ToasterModifier.seqBuild(modifiers)))

  /** Toast an warning with the given modifiers.
    *
    * @example
    * {{{
    *   import services.toaster.ToasterModifierBuilder._
    *   toast.warn("Not that nice!", autoCloseDuration := 2.seconds)
    * }}}
    */
  def warn(content: String, modifiers: ToasterModifier*): URIO[Toaster, Unit] =
    ZIO.serviceWithZIO[Toaster](_.warn(content, ToasterModifier.seqBuild(modifiers)))

  /** Toast an error with the given modifiers.
    *
    * @example
    * {{{
    *   import services.toaster.ToasterModifierBuilder._
    *   toast.error("Uh Oh!", noAutoClose)
    * }}}
    */
  def error(content: String, modifiers: ToasterModifier*): URIO[Toaster, Unit] =
    ZIO.serviceWithZIO[Toaster](_.error(content, ToasterModifier.seqBuild(modifiers)))

}
