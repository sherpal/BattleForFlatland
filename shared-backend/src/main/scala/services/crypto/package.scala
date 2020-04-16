package services

import services.crypto.Crypto.Service
import zio.{Has, URIO, ZIO}

package object crypto {

  type Crypto = Has[Crypto.Service]

  def hashPassword(password: String): URIO[Crypto, HashedPassword] = ZIO.accessM(_.get[Service].hashPassword(password))

  def checkPassword(password: String, hashedPassword: HashedPassword): URIO[Crypto, Boolean] =
    ZIO.accessM(_.get[Service].checkPassword(password, hashedPassword))

  def checkPasswordIfRequired(
      maybePassword: Option[String],
      maybeHashedPassword: Option[HashedPassword]
  ): URIO[Crypto, Boolean] =
    ZIO.accessM(_.get[Service].checkPasswordIfRequired(maybePassword, maybeHashedPassword))

  val uuid: URIO[Crypto, String] = ZIO.accessM(_.get[Service].uuid)

}
