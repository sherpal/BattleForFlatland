package services.i18n

/** A part of a page (e.g. of the tutorial): a heading, then paragraphs (a line starting with "- "
  * is an item of a list).
  */
final case class TextSection(heading: String, paragraphs: Seq[String])
