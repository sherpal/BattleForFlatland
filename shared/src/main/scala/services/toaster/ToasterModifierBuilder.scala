package services.toaster

import zio.ZIO
import utils.ziohelpers.FrontendGlobalEnv

import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

/**
  * A [[ToasterModifierBuilder]] creates instances of [[ToasterModifier]] based on certain values.
  *
  * @example
  * {{{
  *   autoCloseDuration := 2.seconds
  * }}}
  * creates a modifier which makes the toast disappear after 2 seconds.
  *
  * @tparam T type of value required for building the [[ToasterModifier]].
  */
sealed trait ToasterModifierBuilder[T] {

  /** Creates a [[ToasterModifier]] based on the constant value of type T. */
  def :=(value: T): ToasterModifier

  /** Creates a [[ToasterModifier]] based on the constant value None. */
  val default: ToasterModifier
}

object ToasterModifierBuilder {

  implicit def asToasterModifier(builder: ToasterModifierBuilder[Unit]): ToasterModifier = builder := {}

  private def factory[T](modifier: Option[T] => ToastOptions => ToastOptions): ToasterModifierBuilder[T] =
    new ToasterModifierBuilder[T] {
      def :=(value: T): ToasterModifier = (options: ToastOptions) => modifier(Some(value))(options)

      val default: ToasterModifier = (options: ToastOptions) => modifier(Option.empty)(options)
    }

  /** Make the toast disappear after the given duration. */
  val autoCloseDuration = factory[FiniteDuration](maybeDuration => _.copy(autoClose = maybeDuration.map(Right(_))))

  /** Make the toast to never disappear. */
  val noAutoClose = factory[Unit](_ => _.copy(autoClose = Some(Left(false))))

  /** Tells whether to hide the progress bar in the toast. */
  val hideProgressBar = factory[Boolean](maybeBool => _.copy(hideProgressBar = maybeBool))

  /** ZIO effect to apply when the toast disappears. */
  val onClose = factory[ZIO[FrontendGlobalEnv, Nothing, Unit]](maybeEffect => _.copy(onClose = maybeEffect))

}
