package errors

import errors.HTTPResultType.*
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** Parent of all errors that are handled in both the frontend and the backend.
  *
  * This ADT is meant to give more meaningful errors in ZIO error channel, by looking at smaller
  * smallest parent class when resolving errors.
  */
sealed trait ErrorADT extends Throwable {
  def httpErrorType: HTTPResultType

  def json(implicit encoder: Encoder[ErrorADT]): Json = encoder.apply(this)

  def repr: String
}

object ErrorADT {

  final val onlyErrorADT: PartialFunction[Throwable, ErrorADT] = { case e: ErrorADT =>
    e
  }

  type ErrorOr[T] = Either[ErrorADT, T]

  case class MultipleErrors(errors: List[ErrorADT]) extends ErrorADT {
    def httpErrorType: HTTPResultType = errors.headOption.map(_.httpErrorType).getOrElse(Internal)

    def repr: String = errors.map(_.repr).mkString(", ")
  }

  case class MultipleErrorsMap(errors: Map[String, List[ErrorADT]]) extends ErrorADT {
    def httpErrorType: HTTPResultType =
      errors.toList.headOption.flatMap(_._2.headOption).map(_.httpErrorType).getOrElse(Internal)

    def repr: String = errors.toVector
      .map((cat, errors) => s"$cat: ${errors.map(_.repr).mkString(", ")}")
      .mkString("\n")
  }

  case class RawInternalError(errorMsg: String) extends ErrorADT {
    def httpErrorType: HTTPResultType = Internal

    def repr: String = s"Internal: $errorMsg"
  }

  case class RawNotFound() extends ErrorADT {
    def httpErrorType: HTTPResultType = NotFound

    def repr: String = "404 Not Found"
  }

  sealed abstract class ThrowableWrapper(throwable: Throwable) extends Throwable {
    override def getMessage: String                      = throwable.getMessage
    override def getLocalizedMessage: String             = throwable.getLocalizedMessage
    override def getCause: Throwable                     = throwable.getCause
    override def getStackTrace: Array[StackTraceElement] = throwable.getStackTrace
  }

  case class CirceDecodingError(message: String) extends ErrorADT {
    def httpErrorType: HTTPResultType = Internal

    def repr: String = s"Decoding Error: $message"
  }
  def fromCirceDecodingError(error: io.circe.Error): CirceDecodingError = CirceDecodingError(
    error.getMessage + "\n" + error.getStackTrace.map(_.toString).map("\t" + _).mkString("\n")
  )

  sealed trait GameLogicError extends ErrorADT
  case class TooOldActionException(msg: String) extends GameLogicError {
    override def httpErrorType: HTTPResultType = Internal

    def repr: String = s"Action is too old: $msg"
  }

  /** Errors that can be thrown in the backend. */
  sealed trait BackendError extends ErrorADT
  case class WrongStatusCode(code: Int) extends BackendError {
    override def httpErrorType: HTTPResultType = Internal

    def repr: String = toString()
  }

  sealed trait GameServerCredentialsError extends BackendError {
    def repr: String = toString()
  }
  case class MissingGameServerAuthHeader(headerName: String) extends GameServerCredentialsError {
    override def httpErrorType: HTTPResultType = BadRequest
  }
  case object WrongGameCredentials extends GameServerCredentialsError {
    def httpErrorType: HTTPResultType = BadRequest
  }
  case object CouldNotFetchTokenFromGameServer extends GameServerCredentialsError {
    def httpErrorType: HTTPResultType = Internal
  }

  sealed trait DatabaseError extends BackendError
  case class UserExists(userName: String) extends DatabaseError {
    def httpErrorType: HTTPResultType = BadRequest

    def repr: String = s"User $userName already exists."
  }
  case class UserDoesNotExist(userName: String) extends DatabaseError {
    def httpErrorType: HTTPResultType = BadRequest

    def repr: String = s"User $userName does not exists."
  }
  case object CantDeleteTheBoss extends DatabaseError {
    def httpErrorType: HTTPResultType = Forbidden

    def repr: String = toString()
  }
  case class PendingRegistrationNotAdded(userName: String) extends DatabaseError {
    def httpErrorType: HTTPResultType = Internal

    def repr: String = s"Pending Registration for $userName not added."
  }
  case class PendingRegistrationDoesNotExist(registrationKey: String) extends DatabaseError {
    def httpErrorType: HTTPResultType = BadRequest

    def repr: String = s"Pending Registration $registrationKey does not exist."
  }
  case class RawDatabaseError(message: String) extends DatabaseError {
    def httpErrorType: HTTPResultType = Internal

    def repr: String = s"Raw DB Error: $message"
  }

  sealed trait MenuGameError extends BackendError
  case class InconsistentMenuGameInDB(gameId: String, gameName: String) extends MenuGameError {
    def httpErrorType: HTTPResultType = Internal

    def repr: String = s"Inconsistent Menu Game in DB for $gameId:$gameName"
  }
  case class GameExists(gameName: String) extends MenuGameError {
    def httpErrorType: HTTPResultType = BadRequest

    def repr: String = s"Game '$gameName' already exists."
  }
  case class GameDoesNotExist(gameId: String) extends MenuGameError {
    def httpErrorType: HTTPResultType = BadRequest

    def repr: String = s"Game with id $gameId does not exist."
  }
  case class BossDoesNotExist(name: String) extends MenuGameError {
    def httpErrorType: HTTPResultType = BadRequest

    def repr: String = s"Boss $name does not exist."
  }
  case class BossDoesNotHaveAIImplemented(name: String) extends MenuGameError {
    def httpErrorType: HTTPResultType = BadRequest

    def repr: String = s"AIs are not implemented for boss $name"
  }
  case class NoMoreAIAvailable() extends MenuGameError {
    def httpErrorType: HTTPResultType = BadRequest

    def repr: String = s"No AI available left"
  }
  case class GameAlreadyLaunched(gameId: String) extends MenuGameError {
    def httpErrorType: HTTPResultType = BadRequest

    def repr: String = s"Game with id $gameId is already launched."
  }
  case class YouAreNotCreator(userName: String) extends MenuGameError {
    def httpErrorType: HTTPResultType = Forbidden

    def repr: String = s"$userName must be game creator in order to perform this operation."
  }
  case class UserAlreadyPlaying(userName: String) extends MenuGameError {
    def httpErrorType: HTTPResultType = BadRequest

    def repr: String = s"User $userName is already playing."
  }
  case object MissingPassword extends MenuGameError {
    def httpErrorType: HTTPResultType = BadRequest

    def repr: String = "Missing Password"
  }
  case object IncorrectGamePassword extends MenuGameError {
    def httpErrorType: HTTPResultType = BadRequest

    def repr: String = "Wrong game password"
  }
  case class YouAreNotInGame(gameId: String) extends MenuGameError {
    def httpErrorType: HTTPResultType = Forbidden

    def repr: String = s"You are not in game $gameId"
  }
  case object InvalidGameConfiguration extends MenuGameError {
    def httpErrorType: HTTPResultType = BadRequest

    def repr: String = "Invalid Game Configuration"
  }
  case object GameServerLauncherCouldNotBeReached extends MenuGameError {
    def httpErrorType: HTTPResultType = Internal

    def repr: String = toString()
  }

  sealed trait GameAntiChamberError extends BackendError
  case class GameHasBeenCancelled(gameId: String) extends GameAntiChamberError {
    def httpErrorType: HTTPResultType = BadRequest

    def repr: String = s"Game $gameId has been cancelled"
  }

  sealed trait AuthenticationError extends ErrorADT
  case object YouAreUnauthorized extends AuthenticationError {
    def httpErrorType: HTTPResultType = Unauthorized

    def repr: String = s"401 Unauthorized"
  }
  case object ForbiddenForYou extends AuthenticationError {
    def httpErrorType: HTTPResultType = Forbidden

    def repr: String = s"403 Forbidden"
  }
  case object IncorrectPassword extends AuthenticationError {
    override def httpErrorType: HTTPResultType = BadRequest

    def repr: String = "Wrong Password"
  }

  sealed trait FrontendError extends ErrorADT
  case object PasswordsMismatch extends FrontendError {
    def httpErrorType: HTTPResultType = BadRequest

    def repr: String = "Passwords Mismatch"
  }

  sealed trait ValidatorError extends ErrorADT {
    def httpErrorType: HTTPResultType = BadRequest
  }
  sealed trait NumericValidatorError extends ValidatorError
  case class NonZero(value: String) extends NumericValidatorError {
    def repr: String = "Value can't be zero"
  }
  case class NotBiggerThan(value: String, threshold: String) extends NumericValidatorError {
    def repr: String = s"Value $value can't be bigger than $threshold"
  }
  case class NotSmallerThan(value: String, threshold: String) extends NumericValidatorError {
    def repr: String = s"Value $value can't be smaller than $threshold"
  }
  case class Negative(value: String) extends NumericValidatorError {
    def repr: String = s"Value was negative"
  }
  sealed trait StringValidatorError extends ValidatorError {
    def repr: String

    override def getMessage(): String = repr
  }
  case object StringIsEmpty extends StringValidatorError {
    def repr: String = "Can't be empty"
  }
  case class StringIsTooShort(str: String, threshold: Int) extends StringValidatorError {
    def repr: String = s"Must be at least of length $threshold, currently ${str.length}"
  }
  case class ContainsNonLowercaseAlphabet(str: String) extends StringValidatorError {
    def repr: String = "Must contain at least one lowercase letter ([a-z])"
  }
  case class ShouldContain(substr: String, str: String) extends StringValidatorError {
    def repr: String = s"Must contain the following text: $substr"
  }
  case class ShouldNotContain(substr: String, str: String) extends StringValidatorError {
    def repr: String = s"Can't contain the following text: $substr"
  }

  private given Decoder[RawInternalError]    = deriveDecoder
  private given Encoder[RawInternalError]    = deriveEncoder
  private given Decoder[RawNotFound]         = deriveDecoder
  private given Encoder[RawNotFound]         = deriveEncoder
  private given Decoder[FrontendError]       = deriveDecoder
  private given Encoder[FrontendError]       = deriveEncoder
  private given Decoder[CirceDecodingError]  = deriveDecoder
  private given Encoder[CirceDecodingError]  = deriveEncoder
  private given Decoder[GameLogicError]      = deriveDecoder
  private given Encoder[GameLogicError]      = deriveEncoder
  private given Decoder[BackendError]        = deriveDecoder
  private given Encoder[BackendError]        = deriveEncoder
  private given Decoder[AuthenticationError] = deriveDecoder
  private given Encoder[AuthenticationError] = deriveEncoder
  private given Decoder[ValidatorError]      = deriveDecoder
  private given Encoder[ValidatorError]      = deriveEncoder

  given Decoder[ErrorADT] = deriveDecoder
  given Encoder[ErrorADT] = deriveEncoder

}
