package frontend.components.connected.menugames.gamejoined

import frontend.components.Component
import org.scalajs.dom.html
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.laminar.api.L._
import frontend.components.utils.tailwind.{primaryColour, primaryColourDark}
import gamelogic.entities.boss.Boss101
import gamelogic.entities.boss.BossEntity

final class GameOptionPanel private () extends Component[html.Element] {

  val element: ReactiveHtmlElement[html.Element] = section(
    h2(
      className := "text-2xl",
      className := s"text-$primaryColour-$primaryColourDark",
      "Game Options"
    ),
    select(
      BossEntity.allBossesNames.map(name => option(value := name, name)),
      onMountSet(_ => value := BossEntity.allBossesNames.head)
    )
  )

}

object GameOptionPanel {
  def apply(): GameOptionPanel = new GameOptionPanel
}
