package frontend.components.connected.menugames.gamejoined

import frontend.components.Component
import org.scalajs.dom.html
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.laminar.api.L._
import gamelogic.entities.boss.Boss101
import gamelogic.entities.boss.BossEntity

final class GameOptionPanel private () extends Component[html.Element] {

  val element: ReactiveHtmlElement[html.Element] = section(
      select(
          BossEntity.allBossesNames.map(name => option(value := name, name)),
          onMountSet(ctx => value := BossEntity.allBossesNames.head)
      )
  )


}

object GameOptionPanel {
    def apply(): GameOptionPanel = new GameOptionPanel
}