package frontend.router

import com.raquo.airstream.signal.Signal
import com.raquo.laminar.nodes.ReactiveElement
import org.scalajs.dom
import org.scalajs.dom.Element

object Routes {

  def apply(
      routes: List[Route[_ <: dom.Element, _]]
  ): Signal[List[ReactiveElement[Element]]] =
    Router.router.urlStream.map(url => routes.flatMap(_.maybeMakeRenderer(url)).map(_()))

  def apply(routes: Route[_ <: dom.Element, _]*): Signal[List[ReactiveElement[Element]]] = apply(routes.toList)

  def firstOf(routes: List[Route[_ <: dom.Element, _]]): Signal[Option[ReactiveElement[Element]]] =
    Router.router.urlStream.map(url => routes.flatMap(_.maybeMakeRenderer(url)).headOption.map(_()))

  def firstOf(routes: Route[_ <: dom.Element, _]*): Signal[Option[ReactiveElement[Element]]] = firstOf(routes.toList)
}
