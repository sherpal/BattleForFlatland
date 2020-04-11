package services.config

import errors.ErrorADT.ReadingConfigError
import services.config.ConfigRequester.{|>, FromConfig}
import zio.{IO, Layer, ZIO, ZLayer}

object Configuration {

  trait Service {
    //def load[T](configRequester: ConfigRequester)(implicit fromConfig: FromConfig[T]): Task[T]

    def superUserName: IO[ReadingConfigError, String]
    def superUserPassword: IO[ReadingConfigError, String]
    def superUserMail: IO[ReadingConfigError, String]

    def sessionMaxAge: IO[ReadingConfigError, Long]
  }

  val live: Layer[Nothing, Configuration] = ZLayer.succeed(
    new Service {
      def load[T](configRequester: ConfigRequester)(implicit fromConfig: FromConfig[T]): IO[ReadingConfigError, T] =
        ZIO.effect(configRequester.into[T]).refineOrDie {
          case m: ReadingConfigError => m
        }

      def superUserName: IO[ReadingConfigError, String]     = load[String](|> >> "superUser" >> "name")
      def superUserPassword: IO[ReadingConfigError, String] = load[String](|> >> "superUser" >> "password")
      def superUserMail: IO[ReadingConfigError, String]     = load[String](|> >> "superUser" >> "mail")

      def sessionMaxAge: IO[ReadingConfigError, Long] = load[Long](|> >> "play" >> "http" >> "session" >> "maxAge")
    }
  )

}
