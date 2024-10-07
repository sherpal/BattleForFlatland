package components.router

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import org.scalajs.dom.PopStateEvent
import services.routing.baseStr

import scala.concurrent.duration.*
import scala.scalajs.js.timers.setTimeout

final class Router private () {

  import Router.Url

  def url: String = dom.window.location.href

  private lazy val currentUrl: Var[Url] = Var(url)

  private def trigger(): Unit =
    currentUrl.update(_ => url)

  dom.window.addEventListener("popstate", (_: PopStateEvent) => trigger())

  def moveTo(url: String): Unit = {
    val finalUrl =
      if url.startsWith("/") && !url.startsWith(baseStr) then s"$baseStr${url.stripPrefix("/")}"
      else url
    dom.window.history
      .pushState(null, "Title", finalUrl)
    setTimeout(1.millisecond) {
      trigger()
    }
  }

  def currentUrlMatches(matcher: urldsl.language.UrlPart[?, ?]): Boolean =
    matcher.matchRawUrl(url).isRight

  def urlStream: StrictSignal[Url] = currentUrl.signal

}

object Router {

  final val router: Router = new Router()

  type Url = String

}
