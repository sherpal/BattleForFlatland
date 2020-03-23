package services

import zio.Has

package object logging {

  type Logging = Has[Logging.Service]

  val log: Log = new Log

}
