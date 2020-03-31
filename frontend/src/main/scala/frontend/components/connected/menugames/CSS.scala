package frontend.components.connected.menugames

import frontend.GlobalStyleSheet
import scalatags.Text.all._
import scalatags.stylesheet._

object CSS extends StyleSheet with GlobalStyleSheet {
  initStyleSheet()

  val games: Cls = cls(
    backgroundColor := "red"
  )

}
