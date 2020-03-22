package utils

import io.circe._
import io.circe.parser._
import play.api.libs.json.{JsError, JsSuccess, JsValue, Reads}

object ReadsImplicits {

  implicit def reads[A](implicit decoder: Decoder[A]): Reads[A] =
    (json: JsValue) =>
      decode[A](json.toString) match {
        case Right(value) => JsSuccess(value)
        case Left(error)  => JsError(error.getMessage)
      }

}
