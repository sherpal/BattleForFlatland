package bffclient.menu

import scala.concurrent.Future

/** Loads data, one request at a time: a refresh asked during a request is done after it. */
final class Refresh[A](app: MenuApp, load: () => Future[A])(onLoaded: A => Unit) {
  private var loading = false
  private var again   = false

  def apply(): Unit =
    if loading then again = true
    else {
      loading = true
      app.attempt(load())(
        a => { done(); onLoaded(a) },
        _ => done()
      )
    }

  private def done(): Unit = {
    loading = false
    if again then { again = false; apply() }
  }
}
