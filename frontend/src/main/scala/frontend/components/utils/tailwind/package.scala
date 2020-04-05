package frontend.components.utils

import com.raquo.laminar.api.L._

package object tailwind {

  final val btn       = className := "font-bold py-1 px-4 rounded"
  final val btnBlue   = className := "bg-blue-500 text-white hover:bg-blue-700"
  final val btnIndigo = className := "bg-indigo-900 text-white hover:bg-indigo-500"

  final val navbar = className := "flex items-center justify-between flex-wrap bg-indigo-900 text-white"

  final def pad(n: Int): Modifier[HtmlElement] = className := s"p-$n"

  final val cursorPointer = className := "cursor-pointer"

}
