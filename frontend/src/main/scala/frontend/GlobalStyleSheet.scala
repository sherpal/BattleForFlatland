package frontend

import scalatags.stylesheet.StyleSheet

import scala.collection.mutable

trait GlobalStyleSheet { self: StyleSheet =>
  GlobalStyleSheet.sheets += self
}

object GlobalStyleSheet {

  private val sheets: mutable.Set[StyleSheet] = mutable.Set()

  def textStyleSheet: String = sheets.toList.map(sheet => s"""
       |/* --- ${sheet.toString} --- */
       |
       |${sheet.styleSheetText}
       |""".stripMargin).mkString("\n")

}
