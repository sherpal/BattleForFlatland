package components.duringgame

import com.raquo.laminar.api.L.*
import zio.*
import services.FrontendEnv
import menus.data.User
import models.bff.ingame.GameUserCredentials
import services.*
import components.beforegame.GameSettings
import utils.laminarzio.*

object DuringGameMainPage {

  def apply(checkMe: ZIO[FrontendEnv, Nothing, Option[User]], gameId: String)(using
      Runtime[FrontendEnv]
  ): HtmlElement = {
    val loadFixedData = for {
      me <- checkMe
        .someOrFail(IllegalStateException("You are not connected here, something went wrong!"))
        .orDie
      credentials <- localstorage
        .retrieveFrom[GameUserCredentials](
          GameSettings.credentialsStorageKey
        )
        .someOrFail(IllegalStateException(s"No GameUserCredentials could be retrieved!"))
        .orDie
      _ <- ZIO.unless(credentials.gameId == gameId)(
        ZIO.die(
          IllegalStateException(
            s"Stored GameUserCredentials did not match the game id! (${credentials.gameId} != $gameId)"
          )
        )
      )
    } yield (me, credentials)

    div(child <-- EventStream.fromValue(()).flatMapSwitchZIO(_ => loadFixedData).map(withFixedData))
  }
  private def withFixedData(user: User, credentials: GameUserCredentials): HtmlElement =
    div(
      s"$user -- $credentials"
    )

}
