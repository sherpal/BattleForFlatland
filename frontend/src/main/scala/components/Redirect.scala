package components

import com.raquo.laminar.api.L.*
import utils.laminarzio.*
import urldsl.language.PathSegment
import services.FrontendEnv

object Redirect {

  def apply(path: PathSegment[Unit, ?])(using zio.Runtime[FrontendEnv]): HtmlElement = div(
    onMountZIO(services.routing.moveTo(path))
  )
}
