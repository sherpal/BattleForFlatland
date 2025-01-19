package components.router

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveElement
import org.scalajs.dom
import org.scalajs.dom.Element

object Routes {

  def apply(
      routes: Vector[Route[? <: dom.html.Element, ?]]
  ): Signal[Vector[HtmlElement]] =
    Router.router.urlStream.map(url => routes.flatMap(_.maybeMakeRenderer(url)).map(_()))

  def apply(routes: Route[? <: dom.html.Element, ?]*): Signal[Vector[HtmlElement]] = apply(routes.toVector)

  def firstOf(routes: Vector[Route[? <: dom.html.Element, ?]]): Signal[Option[HtmlElement]] =
    Router.router.urlStream.map(url => routes.flatMap(_.maybeMakeRenderer(url)).headOption.map(_()))

  def firstOf(routes: Route[? <: dom.html.Element, ?]*): Signal[Option[HtmlElement]] = firstOf(routes.toVector)
}
