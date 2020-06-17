package frontend.components.connected.menugames.gamejoined

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.domtypes.jsdom.defs.events.TypedTargetMouseEvent
import com.raquo.laminar.api.L._
import com.raquo.laminar.modifiers.EventPropBinder
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import frontend.components.utils.laminarutils.reactChildInDiv
import frontend.components.utils.modal.UnderModalLayer
import frontend.components.utils.tailwind.{primaryColour, primaryColourDark}
import frontend.components.utils.{ColorPickerWrapper, ToggleButton}
import models.bff.outofgame.PlayerClasses
import models.bff.outofgame.gameconfig.PlayerStatus.{NotReady, Ready}
import models.bff.outofgame.gameconfig.{PlayerInfo, PlayerStatus}
import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.html.{Element, Select}
import utils.misc.{RGBAColour, RGBColour}

/**
  * This component is displayed in the GameJoined component, and allows the player to select the game configuration
  * that concerns their entity.
  *
  * That means: their class, their colour...
  *
  * @param initialPlayerInfo the initial player info as it is currently in the database. This is important because the
  *                          player can refresh their screen and need to have up to date information
  * @param playerInfoWriter each time the player makes a change to their character, the new information is sent to the
  *                         server through this [[com.raquo.airstream.core.Observer]], which ends up in the web socket.
  */
final class PlayerInfoOptionPanel private (initialPlayerInfo: PlayerInfo, playerInfoWriter: Observer[PlayerInfo])
    extends Component[html.Element] {

  private var maybeViewChild: Option[html.Element] = Option.empty

  def changeReadyState(ready: PlayerStatus): PlayerInfo => PlayerInfo   = _.copy(status       = ready)
  def changeColour(colour: RGBColour): PlayerInfo => PlayerInfo         = _.copy(playerColour = colour)
  def changeClass(playerClass: PlayerClasses): PlayerInfo => PlayerInfo = _.copy(playerClass  = playerClass)

  val changerBus: EventBus[PlayerInfo => PlayerInfo] = new EventBus
  val readyStateWriter: Observer[PlayerStatus]       = changerBus.writer.contramap(changeReadyState)
  val playerClassWriter: Observer[PlayerClasses]     = changerBus.writer.contramap(changeClass)
  val colourWriter: Observer[RGBAColour]             = changerBus.writer.contramap(changeColour).contramap(_.withoutAlpha)

  val $playerInfo: Signal[PlayerInfo] = changerBus.events.fold(initialPlayerInfo) { (info, changer) =>
    changer(info)
  }

  /**
    * This is the selector for the Player class.
    * Choosing a different option goes to the playerClassWriter above
    */
  val classSelector: ReactiveHtmlElement[Select] = select(
    PlayerClasses.allChoices.map(_.toString).map(cls => option(value := cls, cls)),
    inContext(
      elem =>
        onChange.mapTo(elem.ref.value).map(PlayerClasses.playerClassByName).collect {
          case Some(value) => value
        } --> playerClassWriter
    ),
    onMountSet(_ => value := initialPlayerInfo.playerClass.toString)
  )

  /**
    * Part of the component to chose the colour.
    *
    * This is a simple rectangle on the screen, on which you can click. Clicking on it make the Colour picker appear.
    * Players can then chose a colour they like from the picker.
    */
  val pickerPositionBus: EventBus[(Double, Double)] = new EventBus
  val feedingPickerPosition =
    List(
      onClick
        .map(event => (event.clientX, event.clientY, maybeViewChild))
        .collect {
          case (x, y, Some(viewChild)) => (x, y, viewChild.getBoundingClientRect())
        }
        .map { case (x, y, rect) => (x - rect.left, y - rect.top) } --> pickerPositionBus,
      onClick.mapTo(()) --> UnderModalLayer.showModalWriter
    )

  val $maybePickerPosition: EventStream[Option[(Double, Double)]] = EventStream.merge(
    pickerPositionBus.events.map(Some(_)),
    UnderModalLayer.closeModalEvents.mapTo(Option.empty[(Double, Double)])
  )

  val colourSelector: ReactiveHtmlElement[html.Div] = div(
    "Choose a color: ",
    div(
      height := "30px",
      width := "50px",
      backgroundColor <-- $playerInfo.map(_.playerColour.rgb),
      feedingPickerPosition
    ),
    child <-- $maybePickerPosition.map(_.map {
      case (x, y) =>
        div(
          zIndex := "10",
          position := "absolute",
          left := s"${x + 10}px",
          top := s"${y + 10}px",
          reactChildInDiv(ColorPickerWrapper(colourWriter, initialPlayerInfo.playerColour))
        )
    }).map(_.getOrElse(emptyNode))
  )

  val element: ReactiveHtmlElement[Element] =
    section(
      h2(
        className := "text-2xl",
        className := s"text-$primaryColour-$primaryColourDark",
        "Player Options"
      ),
      "Ready: ",
      ToggleButton(readyStateWriter.contramap(PlayerStatus.fromBoolean), initialPlayerInfo.isReady),
      div(
        "Choose a class:",
        classSelector
      ),
      colourSelector,
      $playerInfo --> playerInfoWriter,
      onMountCallback(ctx => maybeViewChild = Some(ctx.thisNode.ref))
    )
}

object PlayerInfoOptionPanel {
  def apply(initialPlayerInfo: PlayerInfo, playerInfoWriter: Observer[PlayerInfo]): PlayerInfoOptionPanel =
    new PlayerInfoOptionPanel(initialPlayerInfo, playerInfoWriter)
}
