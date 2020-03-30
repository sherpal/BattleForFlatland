package frontend.components.connected.home

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import models.users.User
import org.scalajs.dom.html.Table

object UserList {

  def apply(users: List[User]): ReactiveHtmlElement[Table] = table(
    thead(
      tr(
        th("User name"),
        th("Email"),
        th("Roles"),
        th("Created on"),
        th("Id")
      )
    ),
    tbody(
      users.map {
        case User(userId, userName, _, mailAddress, createdOn, roles) =>
          tr(
            td(userName),
            td(mailAddress),
            td(roles.mkString(", ")),
            td(createdOn.toString),
            td(userId)
          )
      }
    )
  )

}
