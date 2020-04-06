package frontend.components.utils.tailwind.components

import com.raquo.laminar.api.L._

object Table {

  final val tableHeader = className := "px-5 py-3 border-b-2 border-gray-200 bg-gray-100 text-left " +
    "text-xs font-semibold text-gray-600 uppercase tracking-wider"

  final val tableData = className := "p-3 border-b border-gray-200 text-sm " +
    "text-gray-900 whitespace-no-wrap"

  final val clickableRow = className := "hover:bg-gray-300 cursor-pointer"

}
