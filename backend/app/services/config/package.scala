package services

import services.config.ConfigRequester.FromConfig
import zio.{Has, ZIO}

package object config {

  type Configuration = Has[Configuration.Service]

//  def load[T](configRequester: ConfigRequester)(implicit fromConfig: FromConfig[T]): ZIO[Configuration, Throwable, T] =
//    ZIO.accessM(_.get[Configuration.Service].load[T](configRequester))

  def superUserName: ZIO[Configuration, Throwable, String] = ZIO.accessM(_.get[Configuration.Service].superUserName)
  def superUserPassword: ZIO[Configuration, Throwable, String] =
    ZIO.accessM(_.get[Configuration.Service].superUserPassword)
  def superUserMail: ZIO[Configuration, Throwable, String] = ZIO.accessM(_.get[Configuration.Service].superUserMail)

  def sessionMaxAge: ZIO[Configuration, Throwable, Long] = ZIO.accessM(_.get[Configuration.Service].sessionMaxAge)

}
