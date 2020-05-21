package services.database.users

import zio.test.{DefaultRunnableSpec, ZSpec}
import zio._
import zio.console._
import zio.test._
import zio.test.Assertion._
import zio.test.environment._
import zio.random.Random
import zio.test.{Gen, Sized}
import zio.test.Gen._

import services.database.users._
import services.crypto._

object UsersMockupSpec extends DefaultRunnableSpec {

  val layer = zio.clock.Clock.live ++ UsersMockUp.test ++ Crypto.live

  def spec =
    suite("Users mockup should behave")(
      testM("Insert and get user should return true") {
        for {
          userName <- UIO("my name")
          _ <- addUser(userName, "my password", "my address")
          maybeUser <- selectUser(userName).orDie
        } yield assert(maybeUser.isDefined)(equalTo(true))
      },
      testM("Inserting 2 users in the database should return 2 users") {
        for {
          _ <- addUser("hi", "pw", "my address")
          _ <- addUser("hello", "pw", "my other address")
          usrs <- users(0, 2)
        } yield assert(usrs.length)(equalTo(2))
      }
    ).provideLayer(layer)

}
