package services.crypto

import zio.test.{DefaultRunnableSpec, ZSpec}
import zio._
import zio.console._
import zio.test._
import zio.test.Assertion._
import zio.test.environment._
import zio.random.Random
import zio.test.{Gen, Sized}
import zio.test.Gen._

object CryptoSpecs extends DefaultRunnableSpec {

  val cryptoTest: Crypto.Service = new Crypto.Service {
    def hashPassword(password: String): UIO[HashedPassword] = UIO(HashedPassword(password + "_hashed"))

    def checkPassword(password: String, hashedPassword: HashedPassword): UIO[Boolean] =
      hashPassword(password).map(_ == hashedPassword)

    def uuid: UIO[String] = UIO("an-id")
  }

  private val layer = ZLayer.succeed(cryptoTest) ++ Random.live ++ Sized.live(5)

  def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("crypto")(
      testM("checkPasswordIfRequired is consistent in success") {
        for {
          pw <- UIO("hey")
          hashed <- hashPassword(pw)
          check <- checkPasswordIfRequired(Some(pw), Some(hashed))
        } yield assert(check)(equalTo(true))
      },
      testM("checkPasswordIfRequired allows all when password not required") {
        checkM(Gen.alphaNumericString, Gen.boolean) { (pw, absent) =>
          assertM(checkPasswordIfRequired(Some(pw).filter(_ => absent), None))(equalTo(true))
        }
      }
    ).provideLayer(layer)

}
