package components.login

import com.raquo.laminar.api.L.*
import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.ButtonType
import be.doeraene.webcomponents.ui5.configkeys.ButtonDesign
import zio.*
import menus.data.User
import services.FrontendEnv
import models.users.LoginUser
import utils.laminarzio.*
import be.doeraene.webcomponents.ui5.configkeys.ValueState

object Login {

  def apply(
      checkMe: ZIO[FrontendEnv, Nothing, Option[User]]
  )(using Runtime[FrontendEnv]): HtmlElement = {
    val chosenNameVar = Var("")

    val submitBus = new EventBus[Unit]

    def loginEffect(chosenName: String) = (for {
      user        <- ZIO.succeed(User(chosenName))
      maybeErrors <- LoginUser.validator.validateZIO(LoginUser(chosenName, "1234"))
      _ <- maybeErrors match {
        case None         => ZIO.unit
        case Some(errors) => ZIO.fail(errors)
      }
      _ <- services.http.postIgnore(models.users.Routes.login, user).orDie
      _ <- services.routing.moveTo(urldsl.language.dummyErrorImpl.root)
    } yield ()).either

    val loginEvents = submitBus.events
      .sample(chosenNameVar.signal)
      .flatMapSwitchZIO(loginEffect)

    val loginFailures = EventStream.merge(
      loginEvents.collect { case Left(errors) => errors },
      chosenNameVar.signal.changes.mapTo(Map.empty)
    )

    div(
      child <-- EventStream.fromZIO(checkMe).map {
        case None =>
          div(
            Title.h1("Welcome to Battle for Flatland!"),
            form(
              onSubmit.preventDefault --> Observer.empty,
              Label("Please choose a pseudo"),
              Input(
                _.value <-- chosenNameVar.signal.distinct,
                _.events.onInput.map(_.target.value) --> chosenNameVar.writer,
                _.valueState <-- loginFailures
                  .map(_.isEmpty)
                  .map(if _ then ValueState.None else ValueState.Critical),
                _.slots.valueStateMessage <-- loginFailures.map(errors =>
                  errors.get("name") match {
                    case None         => Text("")
                    case Some(errors) => Text(errors.map(_.getMessage()).mkString(", "))
                  }
                )
              ),
              Button(
                marginLeft := "1em",
                "LFG!",
                _.tpe    := ButtonType.Submit,
                _.design := ButtonDesign.Emphasized,
                _.events.onClick.preventDefault.mapToUnit --> submitBus.writer
              )
            ),
            loginEvents --> Observer[Any](x => println(s"Should login as $x"))
          )
        case Some(_) =>
          div(
            onMountZIO(services.routing.moveTo(urldsl.language.dummyErrorImpl.root))
          )
      }
    )
  }

}
