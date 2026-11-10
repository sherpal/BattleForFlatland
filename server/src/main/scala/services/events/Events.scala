package services.events

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference
import scala.reflect.Typeable

/** Dispatches the events to the subscriptions, one event at a time, on its own thread. The
  * subscriptions that are not alive anymore are dropped.
  */
final class Events {

  println("Initializing Events service...")

  private val subscriptionsRef = AtomicReference(Vector.empty[Subscription])
  private val eventQueue       = LinkedBlockingQueue[Event](10000)

  def registerEvents(subscription: Subscription): Unit =
    subscriptionsRef.updateAndGet(_ :+ subscription)

  def registerEvents[InterestedIn <: Event](whileAlive: => Boolean)(using
      Typeable[InterestedIn]
  )(handler: InterestedIn => Unit): Unit =
    registerEvents(
      Subscription(
        {
          case event: InterestedIn => handler(event)
          case _ => ()
        },
        () => whileAlive
      )
    )

  /** Blocks while the queue of the events is full. */
  def dispatchEvent(event: Event): Unit = eventQueue.put(event)

  private def dispatch(event: Event): Unit = {
    println(s"Dispatching $event...")
    val currentSubscriptions         = subscriptionsRef.get
    val numberOfCurrentSubscriptions = currentSubscriptions.length
    val aliveSubscriptions = currentSubscriptions.filter(subscription =>
      try subscription.alive()
      catch { case _: Throwable => false }
    )
    aliveSubscriptions.foreach(subscription =>
      try subscription.handler(event)
      catch {
        case e: Throwable =>
          println("howza! handler failed")
          e.printStackTrace()
      }
    )
    subscriptionsRef.updateAndGet(subscriptionsNow =>
      // we can't simply set, in case a subscription was registered in the meantime
      aliveSubscriptions ++ subscriptionsNow.drop(numberOfCurrentSubscriptions)
    )
  }

  private val dispatcher = Thread(
    () => while true do dispatch(eventQueue.take()),
    "event-dispatcher"
  )
  dispatcher.setDaemon(true)
  dispatcher.start()

  println("Events service initialized.")

}
