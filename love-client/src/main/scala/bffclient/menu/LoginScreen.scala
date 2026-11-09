package bffclient.menu

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import bffclient.controls.ClientSettings
import bffclient.gui.*
import bffclient.love.{Event, LoveSystem}
import bffclient.lua.BuildConfig
import bffclient.ui.{ButtonStyle, Theme}
import models.users.LoginUser

/** Asks for the pseudo, and for the menu server's address in dev: the packages connect to the
  * build's (`BuildConfig.menuServer`), and BFF_MENU_URL, when set, is the one.
  *
  * When the menu server has another version of the game (the packages have one, see
  * `BuildConfig.version`), the player cannot log in: they are told to download it again.
  */
final class LoginScreen(app: MenuApp)(using ExecutionContext) extends Screen {
  private val i18n = app.i18n

  private val fixedServer = app.server.orElse(Option(BuildConfig.menuServer))

  // the server's field, above the pseudo's, when there is one
  private val serverHeight = if fixedServer.isEmpty then 80 else 0

  private var left = false

  private val root = Frame(app.gui.root)
  root.setAllPoints()
  root.enableKeyboard(true)

  private val box = Templates.panel(root)
  box.setSize(440, 210 + serverHeight)
  box.setPoint(Point.Center, root, Point.Center, 0, -15)

  root
    .createFontString(i18n.welcomeToBattleForFlatland, Theme.title)
    .setPoint(Point.Center, box, Point.Top, 0, -65)

  private val form = Frame(box)
  form.setAllPoints()

  private val server = Option.when(fixedServer.isEmpty) {
    form.createFontString(i18n.serverAddress).setPoint(Point.TopLeft, 30, 28)
    // in dev, the menu server runs locally
    val field = EditBox(
      form,
      app.preferences.server.getOrElse("127.0.0.1"),
      i18n.serverAddressExample,
      maxLetters = 64
    )
    field.setSize(380, 38)
    field.setPoint(Point.TopLeft, 30, 56)
    field
  }

  form.createFontString(i18n.choosePseudo).setPoint(Point.TopLeft, 30, 28 + serverHeight)
  private val name = EditBox(form, app.preferences.userName, i18n.yourPseudo, maxLetters = 20)
  name.setSize(380, 38)
  name.setPoint(Point.TopLeft, 30, 56 + serverHeight)

  private val error = form.createFontString(size = Theme.small, colour = Theme.negative)
  error.setWordWrap(true)
  error.setPoint(Point.TopLeft, 30, 102 + serverHeight)
  error.setPoint(Point.TopRight, -30, 102 + serverHeight)

  private val loading = Templates.loading(form, i18n.loggingIn)
  loading.setPoint(Point.TopLeft, 30, 150 + serverHeight)
  loading.hide()

  private val loginButton = Button(form, i18n.enterBattleForFlatland, ButtonStyle.Primary)
  loginButton.setSize(300, 40)
  loginButton.setPoint(Point.TopRight, -30, 140 + serverHeight)

  /** Instead of the form, when the menu server has another version of the game. */
  private val outdated = Frame(box)
  outdated.setAllPoints()
  outdated.hide()

  private val outdatedText = outdated.createFontString(i18n.outdatedVersion)
  outdatedText.setWordWrap(true)
  outdatedText.setJustifyH(JustifyH.Center)
  outdatedText.setPoint(Point.TopLeft, 30, 40)
  outdatedText.setPoint(Point.TopRight, -30, 40)

  private val downloadButton = Button(outdated, i18n.downloadTheNewVersion, ButtonStyle.Primary)
  downloadButton.setSize(300, 40)
  downloadButton.setPoint(Point.Bottom, 0, -40)

  for field <- server do {
    field.setScript(Script.OnEnterPressed)(() =>
      if name.text.trim.isEmpty then name.setFocus() else login()
    )
    field.setScript(Script.OnTabPressed)(() => name.setFocus())
  }
  name.setScript(Script.OnEnterPressed)(() => login())
  name.setScript(Script.OnTabPressed)(() => server.getOrElse(name).setFocus())
  loginButton.setScript(Script.OnClick)(_ => login())
  for field <- fields do field.setScript(Script.OnEscapePressed)(() => Event.quit(0))
  root.setScript(Script.OnKeyDown) {
    case "escape" => Event.quit(0)
    case "tab" => server.getOrElse(name).setFocus()
    case _ => ()
  }

  server.filter(_.text.isEmpty).getOrElse(name).setFocus()

  // told at once, rather than once the player chose their pseudo
  for
    address              <- fixedServer
    (scheme, host, port) <- MenuApi.parseAddress(address, i18n).toOption
  do {
    val api = MenuApi(scheme, host, port)
    for isOutdated <- outdatedFor(api) if isOutdated do showOutdated(api)
  }

  override def leave(): Unit = { left = true; root.remove() }

  private def fields: Seq[EditBox] = server.toSeq :+ name

  private def setBusy(busy: Boolean): Unit = {
    for field <- fields do field.setEnabled(!busy)
    loginButton.setEnabled(!busy)
    loading.setShown(busy)
  }

  /** Whether the menu server has another version of the game than ours. Never in dev (no version),
    * nor when it has none or cannot tell (logging in then tells what is wrong, if anything).
    */
  private def outdatedFor(api: MenuApi): Future[Boolean] =
    BuildConfig.version.toOption.fold(Future.successful(false)) { version =>
      api.clientVersion.map(_.exists(_ != version)).recover { case _ => false }
    }

  /** Tells the player to download the game again, from the menu server's website (in their
    * language).
    */
  private def showOutdated(api: MenuApi): Unit = if !left then {
    val language = ClientSettings.current.language.fold("")(_.code)
    name.clearFocus()
    form.hide()
    outdated.show()
    downloadButton.setScript(Script.OnClick)(_ =>
      LoveSystem.openURL(s"${api.url}/$language#download")
    )
  }

  private def login(): Unit = if loginButton.isEnabled then {
    val pseudo     = name.text.trim
    val nameErrors = LoginUser.validator.validate(LoginUser(pseudo, "1234")).get("name")
    val address    = fixedServer.orElse(server.map(_.text)).getOrElse("")
    (MenuApi.parseAddress(address, i18n), nameErrors) match {
      case (Left(message), _) => error.setText(i18n.serverError(message))
      case (_, Some(errors)) => error.setText(i18n.pseudoError(errors.map(_.repr).mkString(", ")))
      case (Right((scheme, host, port)), None) =>
        setBusy(true)
        error.setText("")
        val api = MenuApi(scheme, host, port)
        outdatedFor(api)
          .flatMap(isOutdated =>
            if isOutdated then Future.successful(None)
            else api.login(pseudo).map(Some(_))
          )
          .onComplete {
            case Success(None) =>
              setBusy(false)
              showOutdated(api)
            case Success(Some(user)) =>
              app.useServer(api)
              app.updatePreferences(preferences =>
                preferences.copy(
                  server = if server.isDefined then Some(api.address) else preferences.server,
                  userName = user.name
                )
              )
              app.show(GamesScreen(app, user))
            case Failure(failure) =>
              setBusy(false)
              name.setFocus()
              error.setText(i18n.cannotLogIn(api.address, failure.getMessage))
          }
    }
  }
}
