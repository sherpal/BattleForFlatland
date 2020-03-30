package services.emails

import scalatags.Text
import zio.Task

object Emails {

  trait Service {

    def sendEmail(recipient: String, subject: String, content: Text.TypedTag[String]): Task[Unit]

  }

}
