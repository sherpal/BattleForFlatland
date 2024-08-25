package entrypoint

import com.raquo.laminar.api.L.*
import be.doeraene.webcomponents.ui5.*
import org.scalajs.dom
import zio.*

@main def run(): Unit = {
  println("Hello Battle for Flatland!")

  val layer = ZLayer.make[services.FrontendEnv](
    services.http.FHttpClient.live
  )

  val runtimeF = Unsafe.unsafe { implicit unsafe =>
    Runtime.unsafe.fromLayer(layer)
  }

  def app = div(
    Title.h1("Battle for Flatland!")
  )

  render(dom.document.getElementById("root"), app)
}
