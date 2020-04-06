package frontend.components.connected.menugames

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import frontend.components.picto.{LockClosed, LockOpen, SmallKey}
import frontend.components.utils.tailwind._
import models.bff.outofgame.MenuGame
import org.scalajs.dom.html
import org.scalajs.dom.html.TableRow
import frontend.components.utils.tailwind.components.Table._

final class DisplayGames private ($games: EventStream[List[MenuGame]], showNewGameWriter: Observer[Unit])
    extends Component[html.Element] {

  def renderGameRow(gameId: String, game: MenuGame, gameStream: EventStream[MenuGame]): ReactiveHtmlElement[TableRow] =
    tr(
      clickableRow,
      td(tableData, child.text <-- gameStream.map(_.gameName)),
      td(tableData, child.text <-- gameStream.map(_.gameCreator.userName)),
      td(
        tableData,
        child <-- gameStream.map(_.maybeHashedPassword.map(_ => SmallKey().element).getOrElse(emptyNode))
      )
    )

  val element: ReactiveHtmlElement[html.Element] = section(
    className := "grid grid-rows-1 grid-cols-5",
    div(
      className := "col-start-2 col-end-5",
      //className := "grid grid-rows-2 grid-cols-1",
      className := "bg-gray-200",
      div(
        className := "flex items-start justify-between border-b-2",
        h1(pad(4), className := "text-xl", "Join a game"),
        button(
          btn,
          primaryButton,
          "New Game",
          onClick.mapTo(()) --> showNewGameWriter
        )
      ),
      div(
        child <-- $games.map {
          case Nil =>
            p(
              pad(2),
              textPrimaryColour,
              "There is currently no game. You can start a new one ",
              span(
                "here",
                textPrimaryColourLight,
                cursorPointer,
                className := s"hover:text-$primaryColour-$primaryColourDark",
                onClick.mapTo(()) --> showNewGameWriter
              ),
              "."
            )
          case _ =>
            table(
              className := "bg-white w-full",
              textPrimaryColour,
              thead(
                tr(
                  th(tableHeader, "Game name"),
                  th(tableHeader, "Created by"),
                  th(tableHeader, "")
                )
              ),
              tbody(
                children <-- $games.split(_.gameId)(renderGameRow)
              )
            )
        }
      )
    )
  )

}

object DisplayGames {
  def apply($games: EventStream[List[MenuGame]], showNewGameWriter: Observer[Unit]) =
    new DisplayGames($games, showNewGameWriter)
}
