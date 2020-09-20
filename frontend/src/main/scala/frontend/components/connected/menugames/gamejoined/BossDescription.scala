package frontend.components.connected.menugames.gamejoined

import com.raquo.airstream.signal.Signal
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import frontend.components.utils.tailwind._
import laika.api.Transformer
import laika.ast._
import laika.format.{HTML, Markdown}
import org.scalajs.dom.html
import org.scalajs.dom.html.Div
import utils.laminarzio.Implicits.zioFlattenStrategy

final class BossDescription private (bossNames: Signal[Option[String]]) extends Component[html.Div] {

  val transformer: Transformer = Transformer
    .from(Markdown)
    .to(HTML)
    .rendering {
      case (fmt, Title(content, opt)) =>
        fmt.element("h1", opt.id.fold[Options](NoOpt)(Id(_)) + Styles("text-3xl", primaryColourDarkClass), content)
      case (fmt, Header(2, content, opt)) =>
        fmt.element("h2", opt.id.fold[Options](NoOpt)(Id(_)) + Styles("text-2xl", primaryColourDarkClass), content)
      case (fmt, Header(3, content, opt)) =>
        fmt.element("h3", opt.id.fold[Options](NoOpt)(Id(_)) + Styles("text-xl", primaryColourDarkClass), content)
      case (fmt, Paragraph(content, options)) =>
        fmt.element("p", options.id.fold[Options](NoOpt)(Id(_)) + Styles("pt-3"), content)
      case (fmt, BulletList(content, _, options)) =>
        fmt.element("ul", options.id.fold[Options](NoOpt)(Id(_)) + Styles("list-disc", "pl-5", "pt-3"), content)
      //      case truc =>
      //        println(truc)
      //        "dummy"
    }
    .build

  val descriptionSignal: EventStream[String] = bossNames
    .flatMap(programs.frontend.gamedocs.bossDescription(_).orDie)
    .map(transformer.transform)
    .map {
      case Left(error) =>
        error.printStackTrace()
        s"<pre>${error.getMessage}</pre>"
      case Right(value) => value
    }

  val element: ReactiveHtmlElement[Div] = div(
    details(
      summary("Boss description"),
      div(onMountCallback { context =>
        descriptionSignal
          .foreach(context.thisNode.ref.innerHTML = _)(context.owner)
      })
    )
  )
}

object BossDescription {
  def apply(bossNames: Signal[Option[String]]): BossDescription = new BossDescription(bossNames)
}
