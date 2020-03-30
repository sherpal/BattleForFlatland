package services.emails

import scalatags.Text
import zio.{Task, ZIO, ZLayer}

object JVMEmails {

  // todo: change impl to actual impl
  final val live = ZLayer.succeed(new Emails.Service {
    def sendEmail(recipient: String, subject: String, content: Text.TypedTag[String]): Task[Unit] =
      ZIO.effect(println(s"I should send an email to $recipient."))
  })

}
