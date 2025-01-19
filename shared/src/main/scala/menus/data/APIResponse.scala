package menus.data

import errors.ErrorADT
import io.circe.Encoder
import io.circe.Decoder
import io.circe.Codec

sealed trait APIResponse[+Data] {
  def toEither: Either[ErrorADT, Data] = this match {
    case APIResponse.Failed(error) => Left(error)
    case APIResponse.Success(data) => Right(data)
  }
}

object APIResponse {

  case class Failed(error: ErrorADT)   extends APIResponse[Nothing]
  case class Success[Data](data: Data) extends APIResponse[Data]

  given [Data](using Encoder[Data], Decoder[Data]): Codec[APIResponse[Data]] =
    io.circe.generic.semiauto.deriveCodec

  def fromEither[A](either: Either[ErrorADT, A]): APIResponse[A] =
    either.fold(Failed(_), Success(_))

}
