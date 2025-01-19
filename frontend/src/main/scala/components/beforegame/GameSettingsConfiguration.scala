package components.beforegame

import com.raquo.laminar.api.L.*
import be.doeraene.webcomponents.ui5.*
import services.FrontendEnv
import models.syntax.Pointed
import models.bff.ingame.GameplaySettings

object GameSettingsConfiguration {

  def apply()(using zio.Runtime[FrontendEnv]): HtmlElement = {
    val gameplaySettingsVar = Var(Pointed[GameplaySettings].unit)

    val volumeUpdater = gameplaySettingsVar.updater[GameplaySettings.Volume]((settings, volume) =>
      settings.copy(volume = volume)
    )

    div(
      Label("Volume"),
      Slider(
        _.min           := 0,
        _.max           := 10,
        _.step          := 1,
        _.showTickmarks := true,
        _.events.onChange
          .map(_.target.value.toInt)
          .map(GameplaySettings.Volume(_)) --> volumeUpdater
      )
    )
  }

}
