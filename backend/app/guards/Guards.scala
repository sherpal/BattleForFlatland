package guards

import java.util.concurrent.TimeUnit

import errors.ErrorADT
import errors.ErrorADT.{CirceDecodingError, ForbiddenForYou, YouAreNotInGame, YouAreUnauthorized}
import io.circe.generic.auto._
import io.circe.parser.decode
import models.users.Role.SuperUser
import models.users.{Role, User}
import play.api.mvc.{Request, RequestHeader, Result}
import services.config._
import services.database.gametables._
import utils.playzio.HasRequest
import utils.playzio.PlayZIO._
import utils.ziohelpers._
import zio.clock.{currentTime, Clock}
import zio.{Has, UIO, ZIO}

object Guards {

  final val userSessionKey: String   = "user"
  final val lastTimestampKey: String = "timestamp"

  /**
    * Adds the session info to the given result.
    */
  def applySession(result: Result, userJson: String, nowAsString: String): Result =
    result.withSession(Guards.userSessionKey -> userJson, Guards.lastTimestampKey -> nowAsString)

  def userFromRequestHeader(req: RequestHeader): ZIO[Clock with Configuration, ErrorADT, User] =
    for {
      maybeUserJson <- UIO(req.session.get(userSessionKey))
      lastTimeStampStr <- UIO(req.session.get(lastTimestampKey))
      lastTimestamp <- ZIO.effect(lastTimeStampStr.get.toLong).refineOrDie(_ => YouAreUnauthorized)
      maxAge <- sessionMaxAge
      now <- currentTime(TimeUnit.SECONDS)
      _ <- if (now - lastTimestamp > maxAge) ZIO.fail(YouAreUnauthorized) else UIO(())
      userJson <- maybeUserJson match {
        case Some(json) => UIO(json)
        case None       => ZIO.fail(ForbiddenForYou)
      }
      user <- ZIO.fromEither(decode[User](userJson)).mapError(_.getMessage).mapError(CirceDecodingError)
    } yield user

  /**
    * Returns the [[SessionRequest]] when the user is properly authenticated within the [[play.api.mvc.Request]]'s
    * session.
    * Fails with a [[errors.ErrorADT.YouAreUnauthorized]] if the request's session does not contain the proper info.
    */
  def authenticated[A](req: Request[A]): ZIO[Clock with Configuration, Throwable, SessionRequest[A]] =
    userFromRequestHeader(req).map(SessionRequest(_, req))

  /**
    * Provides a [[SessionRequest]] when the user is properly authenticated.
    * @tparam A type of the body contained in the request.
    */
  def authenticated[A](
      implicit tagged: zio.Tagged[A]
  ): ZIO[Clock with Configuration with Has[HasRequest[Request, A]], Throwable, SessionRequest[A]] =
    for {
      request <- simpleZIORequest[A]
      sessionRequest <- authenticated[A](request)
    } yield sessionRequest

  /**
    * Returns whether the user in the [[SessionRequest]] has at least one of the given role.
    * Fails with a [[errors.ErrorADT.YouAreUnauthorized]] if it does not.
    */
  def authorized[A, R <: SessionRequest[A]](req: R, roles: List[Role]): ZIO[Any, ErrorADT.YouAreUnauthorized.type, R] =
    ZIO.succeed(req).filterOrFail(_.user.roles.exists(roles.contains))(YouAreUnauthorized)

  /**
    * Returns whether the user in tbe [[SessionRequest]] has at least the given role.
    */
  def authorizedAtLeast[R <: SessionRequest[_]](req: R, role: Role): ZIO[Any, ErrorADT.YouAreUnauthorized.type, R] =
    ZIO.succeed(req).filterOrFail(_.user.roles.exists(_ >= role))(YouAreUnauthorized)

  def onlySuperUser[R <: SessionRequest[_]](req: R): ZIO[Any, ErrorADT.YouAreUnauthorized.type, R] =
    authorizedAtLeast[R](req, SuperUser)

  /**
    * Returns whether the user is authenticated and is playing in the game with that id.
    * Creates a [[guards.JoinedGameRequest]].
    */
  def partOfGame[A](gameId: String)(
      implicit tagged: zio.Tagged[A]
  ): ZIO[GameTable with Clock with Configuration with Has[HasRequest[Request, A]], Throwable, JoinedGameRequest[A]] =
    for {
      sessionRequest <- authenticated[A].refineOrDie(ErrorADT.onlyErrorADT)
      user = sessionRequest.user
      gameInfo <- gameWithPlayersById(gameId)
      isInGame = gameInfo.players.exists(_.userId == user.userId)
      _ <- failIfWith(!isInGame, YouAreNotInGame(gameId))
    } yield JoinedGameRequest[A](gameInfo.onlyPlayerNames, user, sessionRequest.request)

}
