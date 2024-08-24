package entrypoint

import com.raquo.laminar.api.L.*
import be.doeraene.webcomponents.ui5.*
import org.scalajs.dom

@main def run(): Unit = {
  println("Hello Battle for Flatland!")

  def app = div(
    Title.h1("Battle for Flatland!")
  )

  render(dom.document.getElementById("root"), app)
}
