package utils.playzio

import errors.ErrorADT._
import errors.{ErrorADT, HTTPErrorType}
import play.api.mvc.{Result, Results}
import utils.WriteableImplicits._

object ErrorsTransformer extends Results {

  private val httpErrorTypeMap: HTTPErrorType => Status = {
    case HTTPErrorType.BadRequest   => BadRequest
    case HTTPErrorType.Forbidden    => Forbidden
    case HTTPErrorType.Internal     => InternalServerError
    case HTTPErrorType.NotFound     => NotFound
    case HTTPErrorType.Unauthorized => Unauthorized
  }

  implicit final class ErrorADTToResults(errorADT: ErrorADT) {
    def result: Result = httpErrorTypeMap(errorADT.httpErrorType)(errorADT)
  }

}
