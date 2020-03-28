package utils.playzio

import errors.ErrorADT._
import errors.{ErrorADT, HTTPResultType}
import play.api.mvc.{Result, Results}
import utils.WriteableImplicits._

object ErrorsTransformer extends Results {

  private val httpErrorTypeMap: HTTPResultType => Status = {
    case HTTPResultType.BadRequest   => BadRequest
    case HTTPResultType.Forbidden    => Forbidden
    case HTTPResultType.Internal     => InternalServerError
    case HTTPResultType.NotFound     => NotFound
    case HTTPResultType.Unauthorized => Unauthorized
  }

  implicit final class ErrorADTToResults(errorADT: ErrorADT) {
    def result: Result = httpErrorTypeMap(errorADT.httpErrorType)(errorADT)
  }

}
