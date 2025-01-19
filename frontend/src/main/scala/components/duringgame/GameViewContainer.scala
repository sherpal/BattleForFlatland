package components.duringgame

import com.raquo.laminar.api.L.*
import menus.data.User
import gamelogic.entities.Entity
import gamelogic.physics.Complex
import models.bff.ingame.InGameWSProtocol
import com.raquo.airstream.ownership.Owner
import game.GameStateManager
import gamelogic.gamestate.GameState
import models.bff.ingame.Controls
import assets.fonts.Fonts
import org.scalajs.dom

object GameViewContainer {

  def apply(
      me: User,
      playerId: Entity.Id,
      bossStartingPosition: Complex,
      actionsFromServerEvents: EventStream[gamelogic.gamestate.AddAndRemoveActions],
      allPlayersAreReadyEvents: EventStream[Unit],
      socketOutWriter: Observer[InGameWSProtocol.Outgoing],
      deltaTimeWithServer: Long,
      gameId: String,
      controls: Controls,
      fonts: Fonts
  ): HtmlElement = {
    val canvasId = "the-game"
    div(
      display.flex,
      justifyContent.center,
      alignItems.center,
      div(height := "800px", width := "1200px", idAttr := canvasId),
      onMountCallback { ctx =>
        given Owner = ctx.owner

        dom.document.addEventListener("keyup", (event: dom.Event) => event.preventDefault())
        dom.document.addEventListener("keydown", (event: dom.Event) => event.preventDefault())

        println("Creating game state manager")
        GameStateManager(
          me.name,
          GameState.empty,
          actionsFromServerEvents,
          allPlayersAreReadyEvents,
          socketOutWriter,
          playerId,
          bossStartingPosition,
          indigo.Millis(deltaTimeWithServer).toSeconds,
          controls,
          fonts
        ).launch(canvasId, "width" -> "1200", "height" -> "800")
      }
    )
  }

}
