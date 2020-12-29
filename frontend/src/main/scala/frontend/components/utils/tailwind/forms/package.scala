package frontend.components.utils.tailwind

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

package object forms {

  final val formPrimaryTextColour = className := "text-gray-500"

  final val formGroup = className := "md:flex md:items-center mb-6"

  def formLabel(text: String): ReactiveHtmlElement[html.Div] = formLabel(text, (1, 3))

  def formLabel(text: String, widthRatio: (Int, Int)): ReactiveHtmlElement[html.Div] = div(
    className := s"md:w-${widthRatio._1}/${widthRatio._2}",
    label(className := "block font-bold mb-1 md:mb-0 pr-4", formPrimaryTextColour, text)
  )

  def formInput(tpe: String, modifier: Modifier[ReactiveHtmlElement[html.Input]]*): ReactiveHtmlElement[html.Div] = div(
    className := "md:w-2/3",
    input(
      className := "bg-white appearance-none border-gray-400 border-2 rounded w-full " +
        s"py-2 px-4 text-gray-700 leading-tight focus:outline-none focus:bg-white " +
        s"focus:border-$primaryColour-$primaryColourDark",
      `type` := tpe,
      modifier
    )
  )

}
