package services.crypto
import java.util.UUID

import org.mindrot.jbcrypt.BCrypt

object Crypto {

  def hashPassword(password: String): HashedPassword =
    HashedPassword(BCrypt.hashpw(password, BCrypt.gensalt(13)))

  def checkPassword(password: String, hashedPassword: HashedPassword): Boolean =
    BCrypt.checkpw(password, hashedPassword.pw)

  /** Validate the provided `maybePassword` against the `maybeHashedPassword`. If
    * `maybeHashedPassword` is None, then the password is assumed to be *not* required and always
    * validated.
    */
  def checkPasswordIfRequired(
      maybePassword: Option[String],
      maybeHashedPassword: Option[HashedPassword]
  ): Boolean =
    (maybePassword, maybeHashedPassword) match {
      case (_, None) => true
      case (None, Some(_)) => false
      case (Some(password), Some(hashedPassword)) =>
        checkPassword(password, hashedPassword)
    }

  def uuid(): String = UUID.randomUUID().toString

}
