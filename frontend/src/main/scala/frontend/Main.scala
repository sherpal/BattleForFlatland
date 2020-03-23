package frontend

import com.raquo.laminar.api.L._
import org.scalajs.dom
import zio.ZIO

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportTopLevel, JSImport}

@JSImport("resources/index.css", JSImport.Default)
@js.native
object IndexCSS extends js.Object

object Main {
  val css: IndexCSS.type = IndexCSS

  val createElement = ZIO.effect(
    Option(dom.document.getElementById("root")).getOrElse {
      val elem = dom.document.createElement("div")
      elem.id = "root"
      dom.document.body.appendChild(elem)
      elem
    }
  )

  val emptyContainer = for {
    container <- ZIO.environment[dom.Element]
    _ <- ZIO.effect {
      if (scala.scalajs.LinkingInfo.developmentMode) {
        while (container.children.length > 0) {
          container.removeChild(container.children(0))
        }
      }
    }
  } yield ()

  val renderAppInContainer = for {
    container <- ZIO.environment[dom.Element]
    reactiveElement <- ZIO.effect(render(container, App()))
  } yield reactiveElement

  val program = for {
    container <- createElement
    _ <- emptyContainer.provide(container)
    _ <- renderAppInContainer.provide(container)
  } yield ()
  @JSExportTopLevel("main")
  def main(): Unit =
    //program.unsafeRunSync()
    zio.Runtime.default.unsafeRun(program.orDie)
}
