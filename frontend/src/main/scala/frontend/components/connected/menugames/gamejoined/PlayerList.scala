package frontend.components.connected.menugames.gamejoined

import com.raquo.airstream.eventstream.EventStream
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.utils.tailwind.components.Table._
import models.bff.outofgame.gameconfig.PlayerInfo
import org.scalajs.dom.html.Table

object PlayerList {

  def apply($players: EventStream[List[PlayerInfo]]): ReactiveHtmlElement[Table] = table(
    thead(
      tr(
        th(tableHeader, "Name"),
        th(tableHeader, "Colour"),
        th(tableHeader, "Class"),
        th(tableHeader, "Ready?")
      )
    ),
    tbody(
      children <-- $players.map { players =>
        players.map {
          player =>
            tr(
              td(tableData, player.playerName),
              td(
                tableData,
                className := "flex justify-center",
                player.maybePlayerColour
                  .map(colour => span(className := "rounded-full h-4 w-4 flex", backgroundColor := colour.rgb))
              ),
              td(tableData, player.maybePlayerClass.fold("No class")(_.toString)),
              td(
                tableData,
                className := "flex justify-center",
                span(
                  className := "rounded-full h-4 w-4 flex",
                  backgroundColor := (if (player.isReady) "green" else "red")
                )
              )
            )
        }
      }
    )
  )

}
