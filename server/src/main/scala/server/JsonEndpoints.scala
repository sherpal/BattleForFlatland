package server

import cask.model.Response.Raw
import cask.model.Request
import cask.router.Result
import cask.endpoints.QueryParamReader
import io.circe.Encoder
import io.circe.Decoder
import errors.ErrorADT
import menus.data.User

/** Decorators for the users and the bodies of the requests, and endpoints whose results are sent as
  * json.
  */
trait JsonEndpoints {

  def maybeLoggedInUser(ctx: cask.Request) = ctx.cookies.get("session").map(_.value).map(User(_))

  def unauthenticatedResponse =
    cask.Response(ErrorADT.YouAreUnauthorized.json.noSpaces, statusCode = 401)

  class loggedIn extends cask.RawDecorator {
    def wrapFunction(ctx: cask.Request, delegate: Delegate) = maybeLoggedInUser(ctx) match {
      case None => cask.router.Result.Success(unauthenticatedResponse)
      case Some(user) => delegate(Map("user" -> user))
    }
  }

  class readBody[B](using Decoder[B]) extends cask.RawDecorator {
    def wrapFunction(ctx: cask.Request, delegate: Delegate) =
      io.circe.parser.decode[B](ctx.text()) match {
        case Left(err) =>
          cask.router.Result.Success(
            cask.Response(
              ErrorADT.fromCirceDecodingError(err).json.noSpaces,
              statusCode = 400,
              headers = Vector("Content-Type" -> "application/json")
            )
          )
        case Right(body) => delegate(Map("body" -> body))
      }
  }

  sealed trait JsonHttpEndpoint[A](using encoder: Encoder[A])
      extends cask.HttpEndpoint[A, Seq[String]] {
    type InputParser[T] = QueryParamReader[T]

    override def wrapFunction(
        ctx: Request,
        delegate: Map[String, Seq[String]] => Result[A]
    ): Result[Raw] =
      delegate(cask.endpoints.WebEndpoint.buildMapFromQueryParams(ctx)).map(value =>
        cask.Response(
          encoder.apply(value).noSpaces,
          statusCode = 200,
          Vector("Content-Type" -> "application/json"),
          cookies = Vector.empty
        )
      )

    override def wrapPathSegment(s: String): Vector[String] = Vector(s)

  }

  class getJ[A](val path: String)(using Encoder[A]) extends JsonHttpEndpoint[A] {
    override val methods: Seq[String] = Vector("get")
  }

  class postJ[A](val path: String)(using Encoder[A]) extends JsonHttpEndpoint[A] {
    val methods = Vector("post")
  }

}
