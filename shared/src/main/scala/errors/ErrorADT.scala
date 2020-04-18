package errors

import errors.HTTPResultType._
import io.circe.generic.extras.Configuration
import io.circe.{Decoder, Encoder, Json}

/**
  * Parent of all errors that are handled in both the frontend and the backend.
  *
  * This ADT is meant to give more meaningful errors in ZIO error channel, by looking
  * at smaller smallest parent class when resolving errors.
  *
  */
sealed trait ErrorADT extends Throwable {
  def httpErrorType: HTTPResultType

  def json(implicit encoder: Encoder[ErrorADT]): Json = encoder.apply(this)
}

object ErrorADT {

  final val onlyErrorADT: PartialFunction[Throwable, ErrorADT] = {
    case e: ErrorADT => e
  }

  type ErrorOr[T] = Either[ErrorADT, T]

  case class MultipleErrors(errors: List[ErrorADT]) extends ErrorADT {
    def httpErrorType: HTTPResultType = errors.headOption.map(_.httpErrorType).getOrElse(Internal)
  }

  case class MultipleErrorsMap(errors: Map[String, List[ErrorADT]]) extends ErrorADT {
    def httpErrorType: HTTPResultType =
      errors.toList.headOption.flatMap(_._2.headOption).map(_.httpErrorType).getOrElse(Internal)
  }

  sealed abstract class ThrowableWrapper(throwable: Throwable) extends Throwable {
    override def getMessage: String                      = throwable.getMessage
    override def getLocalizedMessage: String             = throwable.getLocalizedMessage
    override def getCause: Throwable                     = throwable.getCause
    override def getStackTrace: Array[StackTraceElement] = throwable.getStackTrace
  }

  case class CirceDecodingError(message: String) extends ErrorADT {
    def httpErrorType: HTTPResultType = Internal
  }
  def fromCirceDecodingError(error: io.circe.Error): CirceDecodingError = CirceDecodingError(
    error.getMessage + "\n" + error.getStackTrace.map(_.toString).map("\t" + _).mkString("\n")
  )

  case class ReadingConfigError(message: String) extends ErrorADT {
    def httpErrorType: HTTPResultType = Internal
  }

  /** Errors that can be thrown in the backend. */
  sealed trait BackendError extends ErrorADT
  case class WrongStatusCode(code: Int) extends BackendError {
    override def httpErrorType: HTTPResultType = Internal
  }

  sealed trait GameServerCredentialsError extends BackendError
  case class MissingGameServerAuthHeader(headerName: String) extends GameServerCredentialsError {
    override def httpErrorType: HTTPResultType = BadRequest
  }
  case object WrongGameCredentials extends GameServerCredentialsError {
    def httpErrorType: HTTPResultType = BadRequest
  }

  sealed trait DatabaseError extends BackendError
  case class UserExists(userName: String) extends DatabaseError {
    def httpErrorType: HTTPResultType = BadRequest
  }
  case class UserDoesNotExist(userName: String) extends DatabaseError {
    def httpErrorType: HTTPResultType = BadRequest
  }
  case object CantDeleteTheBoss extends DatabaseError {
    def httpErrorType: HTTPResultType = Forbidden
  }
  case class PendingRegistrationNotAdded(userName: String) extends DatabaseError {
    def httpErrorType: HTTPResultType = Internal
  }
  case class PendingRegistrationDoesNotExist(registrationKey: String) extends DatabaseError {
    def httpErrorType: HTTPResultType = BadRequest
  }

  sealed trait MenuGameError extends BackendError
  case class InconsistentMenuGameInDB(gameId: String, gameName: String) extends MenuGameError {
    def httpErrorType: HTTPResultType = Internal
  }
  case class GameExists(gameName: String) extends MenuGameError {
    def httpErrorType: HTTPResultType = BadRequest
  }
  case class GameDoesNotExist(gameId: String) extends MenuGameError {
    def httpErrorType: HTTPResultType = BadRequest
  }
  case class UserAlreadyPlaying(userName: String) extends MenuGameError {
    def httpErrorType: HTTPResultType = BadRequest
  }
  case object MissingPassword extends MenuGameError {
    def httpErrorType: HTTPResultType = BadRequest
  }
  case object IncorrectGamePassword extends MenuGameError {
    def httpErrorType: HTTPResultType = BadRequest
  }
  case class YouAreNotInGame(gameId: String) extends MenuGameError {
    def httpErrorType: HTTPResultType = Forbidden
  }

  sealed trait GameAntiChamberError extends BackendError
  case class GameHasBeenCancelled(gameId: String) extends GameAntiChamberError {
    def httpErrorType: HTTPResultType = BadRequest
  }

  sealed trait AuthenticationError extends ErrorADT
  case object YouAreUnauthorized extends AuthenticationError {
    def httpErrorType: HTTPResultType = Unauthorized
  }
  case object ForbiddenForYou extends AuthenticationError {
    def httpErrorType: HTTPResultType = Forbidden
  }
  case object IncorrectPassword extends AuthenticationError {
    override def httpErrorType: HTTPResultType = BadRequest
  }

  sealed trait FrontendError extends ErrorADT
  case object PasswordsMismatch extends FrontendError {
    def httpErrorType: HTTPResultType = BadRequest
  }

  sealed trait ValidatorError extends ErrorADT {
    def httpErrorType: HTTPResultType = BadRequest
  }
  sealed trait NumericValidatorError extends ValidatorError
  case class NonZero(value: String) extends NumericValidatorError
  case class NotBiggerThan(value: String, threshold: String) extends NumericValidatorError
  case class NotSmallerThan(value: String, threshold: String) extends NumericValidatorError
  case class Negative(value: String) extends NumericValidatorError
  sealed trait StringValidatorError extends ValidatorError
  case object StringIsEmpty extends StringValidatorError
  case class StringIsTooShort(str: String, threshold: Int) extends StringValidatorError
  case class ContainsNonLowercaseAlphabet(str: String) extends StringValidatorError
  case class ShouldContain(substr: String, str: String) extends StringValidatorError
  case class ShouldNotContain(substr: String, str: String) extends StringValidatorError

  import io.circe.generic.extras.semiauto._
  implicit val genDevConfig: Configuration =
    Configuration.default.withDiscriminator("what_am_i")

  implicit def decoder: Decoder[ErrorADT] = deriveConfiguredDecoder
  implicit def encoder: Encoder[ErrorADT] = deriveConfiguredEncoder

}
