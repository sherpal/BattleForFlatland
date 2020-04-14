package frontend.components.connected.menugames.gamejoined

import com.raquo.airstream.eventstream.EventStream
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.utils.tailwind.components.Table._
import models.users.User
import org.scalajs.dom.html.Table

object PlayerList {

  def apply($players: EventStream[List[User]]): ReactiveHtmlElement[Table] = table(
    thead(
      tr(
        th(tableHeader, "Name"),
        th(tableHeader, "Status")
      )
    ),
    tbody(
      children <-- $players.map { users =>
        users.map { user =>
          tr(
            td(tableData, user.userName),
            td(tableData, "ready!")
          )
        }
      }
    )
  )

}
