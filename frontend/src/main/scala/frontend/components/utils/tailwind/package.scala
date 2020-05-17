package frontend.components.utils

import com.raquo.laminar.api.L._

package object tailwind {

  final val mainContentContainer = className := "grid grid-rows-1 grid-cols-5"
  final val mainContent          = className := "col-start-2 col-end-5"

  final val primaryColour          = "indigo"
  final val primaryColourDark      = "900"
  final val primaryColourLight     = "500"
  final val primaryColourVeryLight = "200"

  final def pad(n: Int): Modifier[HtmlElement] = className := s"p-$n"
  final def h(n: Int): Modifier[HtmlElement]   = className := s"h-$n"

  final val cursorPointer = className := "cursor-pointer"

  final val textPrimaryColour      = className := s"text-$primaryColour-$primaryColourDark"
  final val textPrimaryColourLight = className := s"text-$primaryColour-$primaryColourLight"

  final val btn     = className := "font-bold py-1 px-4 cursor-pointer"
  final val btnBlue = className := "bg-blue-500 text-white hover:bg-blue-700"

  final val primaryButtonContent =
    s"bg-$primaryColour-$primaryColourDark text-white hover:bg-$primaryColour-$primaryColourLight"
  final val primaryButton = className := primaryButtonContent
  final val primaryButtonDisabledContent =
    s"bg-$primaryColour-$primaryColourDark text-white opacity-50 cursor-not-allowed"
  final val primaryButtonDisabled = className := primaryButtonDisabledContent
  final val secondaryButton       = className := s"text-$primaryColour-$primaryColourLight hover:text-$primaryColour-$primaryColourDark px-4"

  final val headerStyle = className := s"flex items-center justify-between flex-wrap bg-$primaryColour-$primaryColourDark text-white"

}
