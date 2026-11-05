package bffclient.menu

import scala.concurrent.ExecutionContext

import bffclient.controls.ClientSettings
import bffclient.gui.*
import bffclient.love.Event
import bffclient.ui.{ButtonStyle, Theme}
import services.i18n.Language

/** The first screen, until a language is chosen: a button per language, with its name and, under
  * it, "Choose your language" in that language. Choosing one saves it in the settings (where it can
  * be changed later), then goes to the login.
  */
final class LanguageScreen(app: MenuApp)(using ExecutionContext) extends Screen {
  private val buttonWidth = 170.0
  private val gap         = 20.0

  private val root = Frame(app.gui.root)
  root.setAllPoints()
  root.enableKeyboard(true)
  root.setScript(Script.OnKeyDown)(scancode => if scancode == "escape" then Event.quit(0))

  root
    .createFontString("Battle for Flatland", Theme.title)
    .setPoint(Point.Center, root, Point.Center, 0, -120)

  private val row = Frame(root)
  row.setSize(Language.values.length * (buttonWidth + gap) - gap, 90)
  row.setPoint(Point.Center, root, Point.Center, 0, 10)

  for (language, i) <- Language.values.zipWithIndex do {
    val button = Button(row, language.nativeName, ButtonStyle.Primary)
    button.setSize(buttonWidth, 56)
    button.setPoint(Point.TopLeft, i * (buttonWidth + gap), 0)
    button.fontString.setFontSize(Theme.large)
    button.setScript(Script.OnClick)(_ => choose(language))
    row
      .createFontString(language.i18n.chooseYourLanguage, Theme.small, Theme.muted)
      .setPoint(Point.Top, button, Point.Bottom, 0, 10)
  }

  def leave(): Unit = root.remove()

  private def choose(language: Language): Unit = {
    // (if it cannot be saved, it is still the language until the client quits)
    ClientSettings.current
      .copy(language = Some(language))
      .save()
      .foreach(app.notify(_, isError = true))
    app.languageChanged()
    app.show(LoginScreen(app))
  }
}
