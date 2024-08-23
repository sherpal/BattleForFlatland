package services.crypto
import java.util.UUID

import org.mindrot.jbcrypt.BCrypt
import zio.{UIO, ZIO, ZLayer}

object Crypto {

  trait Service {

    def hashPassword(password: String): UIO[HashedPassword]

    def checkPassword(password: String, hashedPassword: HashedPassword): UIO[Boolean]

    /** Validate the provided `maybePassword` against the `maybeHashedPassword`. If `maybeHashedPassword` is None, then
      * the password is assumed to be *not* required and always validated.
      */
    final def checkPasswordIfRequired(
        maybePassword: Option[String],
        maybeHashedPassword: Option[HashedPassword]
    ): UIO[Boolean] =
      (maybePassword, maybeHashedPassword) match {
        case (_, None)       => UIO(true)
        case (None, Some(_)) => UIO(false)
        case (Some(password), Some(hashedPassword)) =>
          checkPassword(password, hashedPassword)
      }

    def uuid: UIO[String]

  }

  final val live: ZLayer[Any, Nothing, Crypto] = ZLayer.succeed(new Service {
    def hashPassword(password: String): UIO[HashedPassword] =
      ZIO.succeed(BCrypt.hashpw(password, BCrypt.gensalt(13))).map(HashedPassword(_))

    def checkPassword(password: String, hashedPassword: HashedPassword): UIO[Boolean] =
      ZIO.succeed(BCrypt.checkpw(password, hashedPassword.pw))

    def uuid: UIO[String] = ZIO.effectTotal(UUID.randomUUID().toString)
  })

}
