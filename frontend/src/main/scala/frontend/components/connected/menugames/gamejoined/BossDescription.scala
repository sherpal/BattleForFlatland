package frontend.components.connected.menugames.gamejoined

import com.raquo.airstream.signal.Signal
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import docs.DocsLoader
import frontend.components.Component
import frontend.components.utils.tailwind._
import laika.api.Transformer
import laika.ast._
import laika.format.{HTML, Markdown}
import org.scalajs.dom.html
import org.scalajs.dom.html.Div

final class BossDescription private (bossNames: Signal[Option[String]]) extends Component[html.Div] {

  val transformer = Transformer
    .from(Markdown)
    .to(HTML)
    .rendering {
      case (fmt, Title(content, opt)) =>
        fmt.element("h1", opt.id.fold[Options](NoOpt)(Id(_)) + Styles("text-3xl", primaryColourDarkClass), content)
      case (fmt, Header(2, content, opt)) =>
        fmt.element("h2", opt.id.fold[Options](NoOpt)(Id(_)) + Styles("text-2xl", primaryColourDarkClass), content)
      case (fmt, Header(3, content, opt)) =>
        fmt.element("h3", opt.id.fold[Options](NoOpt)(Id(_)) + Styles("text-xl", primaryColourDarkClass), content)
      //      case truc =>
      //        println(truc)
      //        "dummy"
    }
    .build

  def bossDescription(maybeBossName: Option[String]): String =
    maybeBossName
      .fold("No boss selected") { bossName =>
        DocsLoader.bossDescription(bossName).getOrElse(s"Description for boss $bossName not found.")
      }

  val element: ReactiveHtmlElement[Div] = div(
    details(
      summary("Boss description"),
      div(onMountCallback { context =>
        bossNames
          .map(bossDescription)
          .map(transformer.transform)
          .map {
            case Left(error) =>
              error.printStackTrace()
              s"<pre>${error.getMessage}</pre>"
            case Right(value) => value
          }
          .foreach(context.thisNode.ref.innerHTML = _)(context.owner)
      })
    )
  )
}

object BossDescription {
  def apply(bossNames: Signal[Option[String]]): BossDescription = new BossDescription(bossNames)
}
