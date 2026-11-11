package be.doeraene.utils.castorutils

import scala.concurrent.{Future, Promise}

extension [T](actor: castor.Actor[T]) {
  def ask[R](replyTo: castor.Actor[R] => T)(using castor.Context): Future[R] = {
    val promise = Promise[R]()
    case class AskActor(a: castor.Actor[T]) extends castor.SimpleActor[R]() {
      override def run(msg: R): Unit = promise.success(msg)
    }
    val receiver = AskActor(actor)
    actor.send(replyTo(receiver))
    promise.future
  }
}
