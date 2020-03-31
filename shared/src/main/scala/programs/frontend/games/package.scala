package programs.frontend

import zio.UIO
import zio.stream._
import zio._
import zio.clock._

import scala.concurrent.duration._

package object games {

  val streamExpl =
    ZStream
      .fromSchedule(Schedule.spaced(zio.duration.Duration.fromScala(2.seconds)))
      .map(e => {
        println("in zstream")
        e
      })

}
