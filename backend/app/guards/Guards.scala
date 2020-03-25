package guards

import java.util.concurrent.TimeUnit

import errors.ErrorADT
import errors.ErrorADT.{ForbiddenForYou, YouAreUnauthorized}
import io.circe.generic.auto._
import io.circe.parser.decode
import models.Role.SuperUser
import models.{Role, User}
import play.api.mvc.{Request, Result}
import services.config._
import zio.ZIO
import zio.clock.{currentTime, Clock}

object Guards {

  final val userSessionKey: String   = "user"
  final val lastTimestampKey: String = "timestamp"

  def applySession(result: Result, userJson: String, nowAsString: String): Result =
    result.withSession(Guards.userSessionKey -> userJson, Guards.lastTimestampKey -> nowAsString)

  def authenticated[A](req: Request[A]): ZIO[Clock with Configuration, Throwable, SessionRequest[A]] =
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

  def authorizedAtLeast[R <: SessionRequest[_]](req: R, role: Role): ZIO[Any, ErrorADT.YouAreUnauthorized.type, R] =
    ZIO.succeed(req).filterOrFail(_.user.roles.exists(_ >= role))(YouAreUnauthorized)

  def onlySuperUser[R <: SessionRequest[_]](req: R): ZIO[Any, ErrorADT.YouAreUnauthorized.type, R] =
    authorizedAtLeast[R](req, SuperUser)

}
