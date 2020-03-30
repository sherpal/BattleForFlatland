package services

import scalatags.Text
import zio.{Has, ZIO}

package object emails {

  type Emails = Has[Emails.Service]

  def sendEmail(recipient: String, subject: String, content: Text.TypedTag[String]): ZIO[Emails, Throwable, Unit] =
    ZIO.accessM(_.get[Emails.Service].sendEmail(recipient, subject, content))

}
