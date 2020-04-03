package frontend

import com.raquo.laminar.api.L._
import org.scalajs.dom
import zio.{UIO, ZIO}

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportTopLevel, JSImport}

@JSImport("resources/index.css", JSImport.Default)
@js.native
object IndexCSS extends js.Object

@JSImport("resources/bootstrap.css", JSImport.Default)
@js.native
object Flatly extends js.Object

object Main {

  println("flatly", Flatly)
  println("css", IndexCSS)

  import typings.popperjsCore.{mod => Popper}
  println("Popper")
  dom.console.log(Popper)

  final val makeCSS = for {
    head <- ZIO.effectTotal(dom.document.getElementsByTagName("head")(0))
    css <- UIO(CSS) *> UIO(GlobalStyleSheet.textStyleSheet) // touching CSS object
    style <- ZIO.effectTotal(dom.document.createElement("style"))
    _ <- ZIO.effectTotal(println(css))
    _ <- ZIO.effectTotal(style.innerText = css)
    _ <- ZIO.effectTotal(head.appendChild(style))
  } yield ()

  final val createElement = ZIO.effect(
    Option(dom.document.getElementById("root")).getOrElse {
      val elem = dom.document.createElement("div")
      elem.id = "root"
      dom.document.body.appendChild(elem)
      elem
    }
  )

  final val emptyContainer = for {
    container <- ZIO.environment[dom.Element]
    _ <- ZIO.effect {
      if (scala.scalajs.LinkingInfo.developmentMode) {
        while (container.children.length > 0) {
          container.removeChild(container.children(0))
        }
      }
    }
  } yield ()

  final val renderAppInContainer = for {
    container <- ZIO.environment[dom.Element]
    reactiveElement <- ZIO.effect(render(container, frontend.components.App()))
    _ <- ZIO.effect(render(container, frontend.components.utils.bootstrap.ModalWindow.modalContainer))
  } yield reactiveElement

  final val program = for {
    container <- createElement
    _ <- emptyContainer.provide(container)
    _ <- renderAppInContainer.provide(container)
    _ <- makeCSS
  } yield ()
  @JSExportTopLevel("main")
  def main(): Unit =
    //program.unsafeRunSync()
    zio.Runtime.default.unsafeRun(program.orDie)
}
