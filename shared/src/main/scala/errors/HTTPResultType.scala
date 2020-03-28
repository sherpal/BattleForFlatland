package errors

sealed trait HTTPResultType

object HTTPResultType {

  case object BadRequest extends HTTPResultType
  case object Internal extends HTTPResultType
  case object Forbidden extends HTTPResultType
  case object NotFound extends HTTPResultType
  case object Unauthorized extends HTTPResultType

}
