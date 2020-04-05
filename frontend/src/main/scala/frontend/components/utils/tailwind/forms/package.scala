package frontend.components.utils.tailwind

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

package object forms {

  final val formGroup = className := "md:flex md:items-center mb-6"

  def formLabel(text: String): ReactiveHtmlElement[html.Div] = div(
    className := "md:w-1/3",
    label(className := "block text-gray-500 font-bold mb-1 md:mb-0 pr-4", text)
  )

  def formInput(tpe: String, modifier: Modifier[ReactiveHtmlElement[html.Input]]*): ReactiveHtmlElement[html.Div] = div(
    className := "md:w-2/3",
    input(
      className := "bg-white appearance-none border-gray-400 border-2 border-white rounded w-full " +
        "py-2 px-4 text-gray-700 leading-tight focus:outline-none focus:bg-white focus:border-indigo-900",
      `type` := tpe,
      modifier
    )
  )

}
