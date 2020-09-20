package gamelogic

/**
  * This package gather some abstract traits whose goal are to normalize documentation and variable calling in various
  * companion object of entities, abilities, buffs...
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

}
