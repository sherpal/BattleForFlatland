package services.http

import org.scalajs.dom.RequestInit
import scala.scalajs.js
import org.scalajs.dom.Response
import scala.scalajs.js.Promise
import org.scalajs.dom

private[http] trait Fetcher {

  def fetch(url: String, requestInit: RequestInit): js.Promise[Response]

}

private[http] object Fetcher {
  def domFetcher = new Fetcher {
    def fetch(url: String, requestInit: RequestInit): Promise[Response] =
      dom.Fetch.fetch(url, requestInit)
  }
}
