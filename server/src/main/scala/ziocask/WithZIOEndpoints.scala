package ziocask

import zio.*
import cask.model.Response.Raw
import cask.model.Request
import cask.router.Result
import cask.endpoints.QueryParamReader
import io.circe.Encoder
import io.circe.Decoder
import errors.ErrorADT
import menus.data.User

trait WithZIOEndpoints[Env] {

  val runtime: Runtime[Env]

  def maybeLoggedInUser(ctx: cask.Request) = ctx.cookies.get("session").map(_.value).map(User(_))

  def unauthenticatedResponse =
    cask.Response(ErrorADT.YouAreUnauthorized.json.noSpaces, statusCode = 401)

  class loggedIn extends cask.RawDecorator {
    def wrapFunction(ctx: cask.Request, delegate: Delegate) = maybeLoggedInUser(ctx) match {
      case None       => cask.router.Result.Success(unauthenticatedResponse)
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

  object caskz {

    sealed trait RawZIOHttpEndpoint[A](using f: A => geny.Writable)
        extends cask.HttpEndpoint[ZIO[Env, Throwable, cask.Response[A]], Seq[String]] {
      type InputParser[T] = QueryParamReader[T]

      override def wrapFunction(
          ctx: Request,
          delegate: Map[String, Seq[String]] => Result[ZIO[Env, Throwable, cask.Response[A]]]
      ): Result[Raw] =
        delegate(cask.endpoints.WebEndpoint.buildMapFromQueryParams(ctx)) match {
          case Result.Success(value) =>
            run(value) match {
              case Exit.Success(value) => Result.Success(value)
              case Exit.Failure(cause) => Result.Error.Exception(cause.squashTrace)
            }
          case error: Result.Error => error
        }

      override def wrapPathSegment(s: String): Vector[String] = Vector(s)

    }

    class get[A](val path: String)(using f: A => geny.Writable) extends RawZIOHttpEndpoint[A] {
      override val methods: Seq[String] = Vector("get")
    }

    class post[A](val path: String)(using f: A => geny.Writable) extends RawZIOHttpEndpoint[A] {
      val methods = Vector("post")
    }

    sealed trait JsonZIOHttpEndpoint[A](using encoder: Encoder[A])
        extends cask.HttpEndpoint[ZIO[Env, Throwable, A], Seq[String]] {
      type InputParser[T] = QueryParamReader[T]

      override def wrapFunction(
          ctx: Request,
          delegate: Map[String, Seq[String]] => Result[ZIO[Env, Throwable, A]]
      ): Result[Raw] =
        delegate(cask.endpoints.WebEndpoint.buildMapFromQueryParams(ctx)) match {
          case Result.Success(value) =>
            run(value) match {
              case Exit.Success(value) =>
                Result.Success(
                  cask.Response(
                    encoder.apply(value).noSpaces,
                    statusCode = 200,
                    Vector("Content-Type" -> "application/json"),
                    cookies = Vector.empty
                  )
                )
              case Exit.Failure(cause) => Result.Error.Exception(cause.squashTrace)
            }
          case error: Result.Error => error
        }

      override def wrapPathSegment(s: String): Vector[String] = Vector(s)

    }

    class getJ[A](val path: String)(using Encoder[A]) extends JsonZIOHttpEndpoint[A] {
      override val methods: Seq[String] = Vector("get")
    }

    class postJ[A](val path: String)(using Encoder[A]) extends JsonZIOHttpEndpoint[A] {
      val methods = Vector("post")
    }

  }

  private def run[A](effect: ZIO[Env, Throwable, A]) = Unsafe.unsafe { implicit unsafe =>
    runtime.unsafe.run(effect)
  }

}
