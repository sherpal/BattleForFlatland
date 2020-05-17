package frontend.components.connected.menugames.gamejoined

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import frontend.components.utils.ToggleButton
import models.bff.outofgame.PlayerClasses
import models.bff.outofgame.gameconfig.PlayerStatus.{NotReady, Ready}
import models.bff.outofgame.gameconfig.{PlayerInfo, PlayerStatus}
import org.scalajs.dom.html
import org.scalajs.dom.html.{Element, Select}
import utils.misc.RGBColour

final class PlayerInfoOptionPanel private (initialPlayerInfo: PlayerInfo, playerInfoWriter: Observer[PlayerInfo])
    extends Component[html.Element] {

  def changeReadyState(ready: PlayerStatus): PlayerInfo => PlayerInfo   = _.copy(status       = ready)
  def changeColour(colour: RGBColour): PlayerInfo => PlayerInfo         = _.copy(playerColour = colour)
  def changeClass(playerClass: PlayerClasses): PlayerInfo => PlayerInfo = _.copy(playerClass  = playerClass)

  val changerBus: EventBus[PlayerInfo => PlayerInfo] = new EventBus
  val readyStateWriter: Observer[PlayerStatus]       = changerBus.writer.contramap(changeReadyState)
  val playerClassWriter: Observer[PlayerClasses]     = changerBus.writer.contramap(changeClass)

  val $playerInfo: Signal[PlayerInfo] = changerBus.events.fold(initialPlayerInfo) { (info, changer) =>
    changer(info)
  }

  val classSelector: ReactiveHtmlElement[Select] = {
    val s = select(
      PlayerClasses.allChoices.map(_.toString).map(cls => option(value := cls, cls)),
      inContext(
        elem =>
          onChange.mapTo(elem.ref.value).map(PlayerClasses.playerClassByName).collect {
            case Some(value) => value
          } --> playerClassWriter
      )
    )
    s.ref.value = initialPlayerInfo.playerClass.toString
    s
  }

  val element: ReactiveHtmlElement[Element] = {
    val elem = section(
      "Ready: ",
      ToggleButton(readyStateWriter.contramap(if (_) Ready else NotReady), initialPlayerInfo.isReady),
      "Chose a class:",
      classSelector
    )

    $playerInfo.foreach(playerInfoWriter.onNext)(elem)

    elem
  }
}

object PlayerInfoOptionPanel {
  def apply(initialPlayerInfo: PlayerInfo, playerInfoWriter: Observer[PlayerInfo]): PlayerInfoOptionPanel =
    new PlayerInfoOptionPanel(initialPlayerInfo, playerInfoWriter)
}
