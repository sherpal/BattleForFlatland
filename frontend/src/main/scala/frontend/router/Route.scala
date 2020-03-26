package frontend.router

import com.raquo.laminar.nodes.ReactiveElement
import frontend.router.Router.Url
import org.scalajs.dom
import urldsl.language.{PathSegment, PathSegmentWithQueryParams}
import urldsl.vocabulary.UrlMatching

final class Route[Ref <: dom.Element, T] private (
    val matcher: Url => Option[T],
    renderer: T => ReactiveElement[Ref]
) {

  def render(url: Url): Option[ReactiveElement[Ref]] =
    matcher(url).map(renderer)

  def maybeMakeRenderer(url: Url): Option[() => ReactiveElement[Ref]] = matcher(url).map(t => () => renderer(t))

}

object Route {

  def apply[T, Ref <: dom.Element](
      pathSegment: PathSegment[T, _],
      renderer: T => ReactiveElement[Ref]
  ): Route[Ref, T] =
    new Route((url: Url) => pathSegment.matchRawUrl(url).toOption, renderer)

  def apply[Ref <: dom.Element](
      pathSegment: PathSegment[Unit, _],
      renderer: () => ReactiveElement[Ref]
  ): Route[Ref, Unit] = apply(pathSegment, (_: Unit) => renderer())

  def apply[T, Q, Ref <: dom.Element](
      pathSegmentWithQueryParams: PathSegmentWithQueryParams[T, _, Q, _],
      renderer: (T, Q) => ReactiveElement[Ref]
  ): Route[Ref, UrlMatching[T, Q]] = new Route(
    (url: Url) => pathSegmentWithQueryParams.matchRawUrl(url).toOption,
    (matching: UrlMatching[T, Q]) => renderer(matching.path, matching.params)
  )

//  def apply[Q, Ref <: dom.Element](
//      pathSegmentWithQueryParams: PathSegmentWithQueryParams[Unit, _, Q, _],
//      renderer: Q => ReactiveElement[Ref]
//  )(implicit dummyImplicit: DummyImplicit): Route[Ref, Q] = new Route(
//    (url: Url) => pathSegmentWithQueryParams.matchRawUrl(url).toOption.map(_.params),
//    renderer
//  )

}
