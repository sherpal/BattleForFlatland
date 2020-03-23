package utils.laminarzio

import com.raquo.airstream.eventstream.EventStream
import zio.ZIO

object Implicits {

  implicit class EventStreamObjEnhanced(es: EventStream.type) {

    def fromZIOEffect[R](effect: ZIO[Any, Throwable, R]): EventStream[R] =
      EventStream.fromFuture(zio.Runtime.default.unsafeRunToFuture(effect))

  }

}
