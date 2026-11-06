package services.i18n

/** The languages of the UI: each with its code (e.g. in the settings' file), its name in itself,
  * and its texts.
  */
enum Language(val code: String, val nativeName: String, val i18n: I18n) {
  case English extends Language("en", "English", EnglishI18n)
  case French extends Language("fr", "Français", FrenchI18n)
  case Dutch extends Language("nl", "Nederlands", DutchI18n)
  case German extends Language("de", "Deutsch", GermanI18n)
  case Albanian extends Language("sq", "Shqip", AlbanianI18n)
}

object Language {
  def byCode(code: String): Option[Language] = values.find(_.code == code)
}
