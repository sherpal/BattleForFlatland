package services.crypto

import zio.{URIO, ZIO}

def hashPassword(password: String): URIO[Crypto, HashedPassword] = ZIO.serviceWithZIO[Crypto](_.hashPassword(password))

def checkPassword(password: String, hashedPassword: HashedPassword): URIO[Crypto, Boolean] =
  ZIO.serviceWithZIO[Crypto](_.checkPassword(password, hashedPassword))

def checkPasswordIfRequired(
    maybePassword: Option[String],
    maybeHashedPassword: Option[HashedPassword]
): URIO[Crypto, Boolean] =
  ZIO.serviceWithZIO[Crypto](_.checkPasswordIfRequired(maybePassword, maybeHashedPassword))

val uuid: URIO[Crypto, String] = ZIO.serviceWithZIO[Crypto](_.uuid)
