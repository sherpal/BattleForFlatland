package utils.laminarzio

import com.raquo.airstream.core.Observable
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.features.FlattenStrategy
import zio.stream._
import zio.{CancelableFuture, UIO, ZIO}

object Implicits {

  implicit val zioFlattenStrategy: FlattenStrategy[Observable, UIO, EventStream] =
    new FlattenStrategy[Observable, UIO, EventStream] {
      def flatten[A](parent: Observable[UIO[A]]): EventStream[A] =
        parent.flatMap(
          task =>
            EventStream.fromFuture(
              zio.Runtime.default.unsafeRunToFuture(task)
            )
        )
    }

  implicit class EventStreamObjEnhanced(es: EventStream.type) {

    /**
      * Retrieve the result of the zio effect and send it through the laminar stream
      */
    def fromZIOEffect[A](effect: ZIO[Any, Throwable, A]): EventStream[A] =
      EventStream.fromFuture(zio.Runtime.default.unsafeRunToFuture(effect))

    /**
      * Passes the outputs of the incoming [[zio.stream.ZStream]] into a laminar stream.
      * /!\ The ZStream will continue to run, even after the laminar stream is over with it. The returned
      * [[zio.CancelableFuture]] allows you to cancel it.
      *
      * I think this is not fantastic. We should do it in another way, probably.
      */
    def fromZStream[A](ztream: Stream[Nothing, A]): (CancelableFuture[Unit], EventStream[A]) = {
      val bus = new EventBus[A]
      zio.Runtime.default.unsafeRunToFuture(ztream.foreach(elem => ZIO.effectTotal(bus.writer.onNext(elem)))) ->
        bus.events
    }

  }

}
