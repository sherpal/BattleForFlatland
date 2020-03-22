package guards

import java.util.concurrent.TimeUnit

import errors.ErrorADT
import errors.ErrorADT.{ForbiddenForYou, YouAreUnauthorized}
import play.api.mvc.{Request, Result}
import zio.{UIO, ZIO}
import io.circe.generic.auto._
import io.circe.parser.decode
import services.config._
import zio.clock.{currentTime, Clock}
import models.{Role, User}

object Guards {

  final val userSessionKey: String   = "user"
  final val lastTimestampKey: String = "timestamp"

  def applySession(result: Result, userJson: String, nowAsString: String): Result =
    result.withSession(Guards.userSessionKey -> userJson, Guards.lastTimestampKey -> nowAsString)

  def authenticated[A, R <: Request[A]](req: R): ZIO[Clock with Configuration, Throwable, SessionRequest[A]] =
    for {
      maybeUserJson <- ZIO.succeed(req.session.get(userSessionKey))
      lastTimeStampStr <- ZIO.succeed(req.session.get(lastTimestampKey))
      lastTimestamp <- ZIO.effect(lastTimeStampStr.get.toLong / 1000).refineOrDie { case _ => YouAreUnauthorized }
      maxAge <- sessionMaxAge
      now <- currentTime(TimeUnit.SECONDS)
      _ <- if (now - lastTimestamp > maxAge) ZIO.fail(YouAreUnauthorized) else ZIO.succeed(())
      userJson <- maybeUserJson match {
        case Some(json) => ZIO.succeed(json)
        case None       => ZIO.fail(ForbiddenForYou)
      }
      user <- ZIO.fromEither(decode[User](userJson))
    } yield SessionRequest(user, req)

  def authorized[A, R <: SessionRequest[A]](req: R, roles: List[Role]): ZIO[Any, ErrorADT.YouAreUnauthorized.type, R] =
    ZIO.succeed(req).filterOrFail(_.user.roles.exists(roles.contains))(YouAreUnauthorized)

  def authorizedAtLeast[A, R <: SessionRequest[A]](req: R, role: Role): ZIO[Any, ErrorADT.YouAreUnauthorized.type, R] =
    ZIO.succeed(req).filterOrFail(_.user.roles.exists(_ >= role))(YouAreUnauthorized)

}
