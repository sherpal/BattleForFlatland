package components.router

import com.raquo.laminar.nodes.ReactiveHtmlElement
import components.router.Router.Url
import org.scalajs.dom
import urldsl.language.{PathSegment, PathSegmentWithQueryParams}
import urldsl.vocabulary.UrlMatching

final class Route[Ref <: dom.html.Element, T] private (
    val matcher: Url => Option[T],
    renderer: T => ReactiveHtmlElement[Ref]
) {

  def render(url: Url): Option[ReactiveHtmlElement[Ref]] =
    matcher(url).map(renderer)

  def maybeMakeRenderer(url: Url): Option[() => ReactiveHtmlElement[Ref]] =
    matcher(url).map(t => () => renderer(t))

}

object Route {

  def apply[T, Ref <: dom.html.Element](
      pathSegment: PathSegment[T, ?],
      renderer: T => ReactiveHtmlElement[Ref]
  ): Route[Ref, T] =
    new Route(
      (url: Url) => pathSegment.matchRawUrl(url).toOption,
      renderer
    )

  def apply[Ref <: dom.html.Element](
      pathSegment: PathSegment[Unit, ?],
      renderer: () => ReactiveHtmlElement[Ref]
  ): Route[Ref, Unit] = apply(pathSegment, (_: Unit) => renderer())

  def apply[T, Q, Ref <: dom.html.Element](
      pathSegmentWithQueryParams: PathSegmentWithQueryParams[T, ?, Q, ?],
      renderer: (T, Q) => ReactiveHtmlElement[Ref]
  ): Route[Ref, UrlMatching[T, Q]] = new Route(
    (url: Url) => pathSegmentWithQueryParams.matchRawUrl(url).toOption,
    (matching: UrlMatching[T, Q]) => renderer(matching.path, matching.params)
  )

  def matchOnly[T, Q, Ref <: dom.html.Element](
      pathSegmentWithQueryParams: PathSegmentWithQueryParams[T, ?, Q, ?],
      renderer: () => ReactiveHtmlElement[Ref]
  ): Route[Ref, Any] = new Route(
    (url: Url) => pathSegmentWithQueryParams.matchRawUrl(url).toOption,
    (_: Any) => renderer()
  )

}
