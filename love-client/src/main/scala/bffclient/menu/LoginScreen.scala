package bffclient.menu

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

import bffclient.gui.*
import bffclient.love.Event
import bffclient.lua.BuildConfig
import bffclient.ui.{ButtonStyle, Theme}
import models.users.LoginUser

final class LoginScreen(app: MenuApp)(using ExecutionContext) extends Screen {
  private val i18n = app.i18n

  private val root = Frame(app.gui.root)
  root.setAllPoints()
  root.enableKeyboard(true)

  private val box = Templates.panel(root)
  box.setSize(440, 290)
  box.setPoint(Point.Center, root, Point.Center, 0, -15)

  root
    .createFontString(i18n.welcomeToBattleForFlatland, Theme.title)
    .setPoint(Point.Center, box, Point.Top, 0, -65)

  box.createFontString(i18n.serverAddress).setPoint(Point.TopLeft, 30, 28)
  private val server = EditBox(
    box,
    app.server.orElse(app.preferences.server).orElse(Option(BuildConfig.menuServer)).getOrElse(""),
    i18n.serverAddressExample,
    maxLetters = 64
  )
  server.setSize(380, 38)
  server.setPoint(Point.TopLeft, 30, 56)

  box.createFontString(i18n.choosePseudo).setPoint(Point.TopLeft, 30, 108)
  private val name = EditBox(box, app.preferences.userName, i18n.yourPseudo, maxLetters = 20)
  name.setSize(380, 38)
  name.setPoint(Point.TopLeft, 30, 136)

  private val error = box.createFontString(size = Theme.small, colour = Theme.negative)
  error.setWordWrap(true)
  error.setPoint(Point.TopLeft, 30, 182)
  error.setPoint(Point.TopRight, -30, 182)

  private val loading = Templates.loading(box, i18n.loggingIn)
  loading.setPoint(Point.TopLeft, 30, 230)
  loading.hide()

  private val loginButton = Button(box, i18n.enterBattleForFlatland, ButtonStyle.Primary)
  loginButton.setSize(300, 40)
  loginButton.setPoint(Point.TopRight, -30, 220)

  server.setScript(Script.OnEnterPressed)(() =>
    if name.text.trim.isEmpty then name.setFocus() else login()
  )
  server.setScript(Script.OnTabPressed)(() => name.setFocus())
  name.setScript(Script.OnEnterPressed)(() => login())
  name.setScript(Script.OnTabPressed)(() => server.setFocus())
  loginButton.setScript(Script.OnClick)(_ => login())
  for field <- Seq(server, name) do field.setScript(Script.OnEscapePressed)(() => Event.quit(0))
  root.setScript(Script.OnKeyDown) {
    case "escape" => Event.quit(0)
    case "tab" => server.setFocus()
    case _ => ()
  }

  (if server.text.isEmpty then server else name).setFocus()

  override def leave(): Unit = root.remove()

  private def setBusy(busy: Boolean): Unit = {
    for field <- Seq(server, name) do field.setEnabled(!busy)
    loginButton.setEnabled(!busy)
    loading.setShown(busy)
  }

  private def login(): Unit = if loginButton.isEnabled then {
    val pseudo     = name.text.trim
    val nameErrors = LoginUser.validator.validate(LoginUser(pseudo, "1234")).get("name")
    (MenuApi.parseAddress(server.text, i18n), nameErrors) match {
      case (Left(message), _) => error.setText(i18n.serverError(message))
      case (_, Some(errors)) => error.setText(i18n.pseudoError(errors.map(_.repr).mkString(", ")))
      case (Right((scheme, host, port)), None) =>
        setBusy(true)
        error.setText("")
        val api = MenuApi(scheme, host, port)
        api.login(pseudo).onComplete {
          case Success(user) =>
            app.useServer(api)
            app.updatePreferences(_.copy(server = Some(api.address), userName = user.name))
            app.show(GamesScreen(app, user))
          case Failure(failure) =>
            setBusy(false)
            name.setFocus()
            error.setText(i18n.cannotLogIn(api.address, failure.getMessage))
        }
    }
  }
}
