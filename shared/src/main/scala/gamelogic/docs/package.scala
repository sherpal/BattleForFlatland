package gamelogic

/** This package gather some abstract traits whose goal are to normalize documentation and variable
  * calling in various companion object of entities, abilities, buffs...
  */
package object docs {

  def formatSeconds(millis: Long): String =
    if (millis % 1000L == 0) {
      val seconds = millis / 1000L

      s"$seconds second${if (seconds > 1) "s" else ""}"
    } else {
      val seconds = millis / 1000.0

      s"$seconds second${if (seconds > 1) "s" else ""}"
    }

  /** Formats a period for display after "every", so that it reads "every second" rather than "every
    * 1 second".
    */
  def formatPeriod(millis: Long): String =
    if (millis == 1000L) "second" else formatSeconds(millis)

  /** Formats a number for display in docs, dropping the decimal part when there is none. */
  def formatNumber(x: Double): String =
    if (x.isWhole) x.toLong.toString
    else
      BigDecimal(x)
        .setScale(2, BigDecimal.RoundingMode.HALF_UP)
        .bigDecimal
        .stripTrailingZeros
        .toPlainString

}
