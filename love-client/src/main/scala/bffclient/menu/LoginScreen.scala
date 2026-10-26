package bffclient.menu

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

import bffclient.love.Event
import bffclient.ui.*
import models.users.LoginUser

final class LoginScreen(app: MenuApp)(using ExecutionContext) extends Screen {
  private val server =
    TextInput(app.server.orElse(app.preferences.server).getOrElse(""), maxLength = 64)
  private val name  = TextInput(app.preferences.userName, maxLength = 20)
  private var busy  = false
  private var error = Option.empty[String]
  app.ui.setFocus(if server.text.isEmpty then server else name)

  def draw(ui: Ui): Unit = {
    val (width, height) = app.size
    val box             = Rect(width / 2 - 220, height / 2 - 160, 440, 290)
    ui.textIn("Welcome to Battle for Flatland!", Rect(0, box.y - 90, width, 50), Theme.title)
    ui.panel(box)
    ui.text("Server address", box.x + 30, box.y + 28)
    val serverEntered = ui.textField(
      server,
      Rect(box.x + 30, box.y + 56, 380, 38),
      s"e.g. 81.12.34.56:${MenuApi.defaultPort}",
      enabled = !busy
    )
    ui.text("Please choose a pseudo", box.x + 30, box.y + 108)
    val nameEntered =
      ui.textField(name, Rect(box.x + 30, box.y + 136, 380, 38), "Your pseudo", enabled = !busy)
    error.foreach(message =>
      ui.paragraph(message, box.x + 30, box.y + 182, 380, Theme.small, Theme.negative)
    )
    if busy then ui.loading("Logging in", box.x + 30, box.y + 230)
    val clicked = ui.button(
      "LFG!",
      Rect(box.right - 150, box.y + 220, 120, 40),
      enabled = !busy,
      ButtonStyle.Primary
    )
    if ui.wasPressed("tab") then ui.setFocus(if ui.focused(server) then name else server)
    if serverEntered && name.text.trim.isEmpty then ui.setFocus(name)
    else if (serverEntered || nameEntered || clicked) && !busy then login()
    if ui.wasPressed("escape") then Event.quit(0)
  }

  private def login(): Unit = {
    val pseudo     = name.text.trim
    val nameErrors = LoginUser.validator.validate(LoginUser(pseudo, "1234")).get("name")
    (MenuApi.parseAddress(server.text), nameErrors) match {
      case (Left(message), _) => error = Some(s"Server: $message")
      case (_, Some(errors)) => error = Some(s"Pseudo: ${errors.map(_.repr).mkString(", ")}")
      case (Right((host, port)), None) =>
        busy = true
        error = None
        val api = new MenuApi(host, port)
        api.login(pseudo).onComplete {
          case Success(user) =>
            app.useServer(api)
            app.updatePreferences(_.copy(server = Some(api.address), userName = user.name))
            app.show(new GamesScreen(app, user))
          case Failure(failure) =>
            busy = false
            error = Some(s"Cannot log in to ${api.address}: ${failure.getMessage}")
        }
    }
  }
}
