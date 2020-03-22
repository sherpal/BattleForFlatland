package utils

import io.circe.Encoder
import io.circe.syntax._
import play.api.http.{ContentTypeOf, ContentTypes, Writeable}
import play.api.libs.json.Json
import play.api.mvc.Codec

trait WriteableImplicits {
//  implicit def jsonWritable[A](implicit writes: Writes[A], codec: Codec): Writeable[A] = {
//    implicit val contentType: ContentTypeOf[A] = ContentTypeOf[A](Some(ContentTypes.JSON))
//    val transform = Writeable.writeableOf_JsValue.transform compose writes.writes
//    Writeable(transform)
//  }

  implicit def jsonWritable[A](implicit writes: Encoder[A], codec: Codec): Writeable[A] = {
    implicit val contentType: ContentTypeOf[A] = ContentTypeOf[A](Some(ContentTypes.JSON))
    val transform = Writeable.writeableOf_JsValue.transform compose (Json
      .parse(_: String)) compose ((_: A).asJson.noSpaces)
    Writeable(transform)
  }
}

object WriteableImplicits extends WriteableImplicits
