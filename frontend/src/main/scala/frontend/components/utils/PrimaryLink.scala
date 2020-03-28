package frontend.components.utils

import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.router.{Link, Router}
import org.scalajs.dom.html
import urldsl.language.{PathSegment, PathSegmentWithQueryParams, QueryParameters}
import com.raquo.laminar.api.L._

object PrimaryLink {
  def apply(to: PathSegment[Unit, _])(text: String): ReactiveHtmlElement[html.Span] =
    span(className := "primary-link clickable", Link(to)(text))

  def apply[Q](to: PathSegmentWithQueryParams[Unit, _, Q, _], q: Q)(text: String): ReactiveHtmlElement[html.Span] =
    span(className := "primary-link clickable", Link(to, q)(text))

  def apply[Q](to: PathSegment[Unit, _], withParams: QueryParameters[Q, _], q: Q)(
      text: String
  ): ReactiveHtmlElement[html.Span] =
    span(className := "primary-link clickable", Link(to, withParams, q)(text))

}
