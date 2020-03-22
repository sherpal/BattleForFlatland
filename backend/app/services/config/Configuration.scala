package services.config

import services.config.ConfigRequester.{|>, FromConfig}
import zio.{Layer, Task, ZIO, ZLayer}

object Configuration {

  trait Service {
    //def load[T](configRequester: ConfigRequester)(implicit fromConfig: FromConfig[T]): Task[T]

    def superUserName: Task[String]
    def superUserPassword: Task[String]
    def superUserMail: Task[String]

    def sessionMaxAge: Task[Long]
  }

  val live: Layer[Nothing, Configuration] = ZLayer.succeed(
    new Service {
      def load[T](configRequester: ConfigRequester)(implicit fromConfig: FromConfig[T]): Task[T] =
        ZIO.effect(configRequester.into[T])

      def superUserName: Task[String]     = load[String](|> >> "superUser" >> "name")
      def superUserPassword: Task[String] = load[String](|> >> "superUser" >> "password")
      def superUserMail: Task[String]     = load[String](|> >> "superUser" >> "mail")

      def sessionMaxAge: Task[Long] = load[Long](|> >> "play" >> "http" >> "session" >> "maxAge")
    }
  )

}
