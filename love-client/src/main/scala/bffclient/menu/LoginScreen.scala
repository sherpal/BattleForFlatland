package bffclient.menu

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

import bffclient.gui.*
import bffclient.love.Event
import bffclient.ui.{ButtonStyle, Theme}
import models.users.LoginUser

final class LoginScreen(app: MenuApp)(using ExecutionContext) extends Screen {
  private val root = Frame(app.gui.root)
  root.setAllPoints()
  root.enableKeyboard(true)

  private val box = Templates.panel(root)
  box.setSize(440, 290)
  box.setPoint(Point.Center, root, Point.Center, 0, -15)

  root
    .createFontString("Welcome to Battle for Flatland!", Theme.title)
    .setPoint(Point.Center, box, Point.Top, 0, -65)

  box.createFontString("Server address").setPoint(Point.TopLeft, 30, 28)
  private val server = EditBox(
    box,
    app.server.orElse(app.preferences.server).getOrElse(""),
    s"e.g. 81.12.34.56:${MenuApi.defaultPort}",
    maxLetters = 64
  )
  server.setSize(380, 38)
  server.setPoint(Point.TopLeft, 30, 56)

  box.createFontString("Please choose a pseudo").setPoint(Point.TopLeft, 30, 108)
  private val name = EditBox(box, app.preferences.userName, "Your pseudo", maxLetters = 20)
  name.setSize(380, 38)
  name.setPoint(Point.TopLeft, 30, 136)

  private val error = box.createFontString(size = Theme.small, colour = Theme.negative)
  error.setWordWrap(true)
  error.setPoint(Point.TopLeft, 30, 182)
  error.setPoint(Point.TopRight, -30, 182)

  private val loading = Templates.loading(box, "Logging in")
  loading.setPoint(Point.TopLeft, 30, 230)
  loading.hide()

  private val loginButton = Button(box, "LFG!", ButtonStyle.Primary)
  loginButton.setSize(120, 40)
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
    (MenuApi.parseAddress(server.text), nameErrors) match {
      case (Left(message), _) => error.setText(s"Server: $message")
      case (_, Some(errors)) => error.setText(s"Pseudo: ${errors.map(_.repr).mkString(", ")}")
      case (Right((host, port)), None) =>
        setBusy(true)
        error.setText("")
        val api = new MenuApi(host, port)
        api.login(pseudo).onComplete {
          case Success(user) =>
            app.useServer(api)
            app.updatePreferences(_.copy(server = Some(api.address), userName = user.name))
            app.show(new GamesScreen(app, user))
          case Failure(failure) =>
            setBusy(false)
            name.setFocus()
            error.setText(s"Cannot log in to ${api.address}: ${failure.getMessage}")
        }
    }
  }
}
