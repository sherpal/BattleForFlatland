package errors

sealed trait HTTPErrorType

object HTTPErrorType {

  case object BadRequest extends HTTPErrorType
  case object Internal extends HTTPErrorType
  case object Forbidden extends HTTPErrorType
  case object NotFound extends HTTPErrorType
  case object Unauthorized extends HTTPErrorType

}
