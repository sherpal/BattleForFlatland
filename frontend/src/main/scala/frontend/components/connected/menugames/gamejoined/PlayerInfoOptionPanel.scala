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
import io.circe.{Decoder, Encoder}
import models.bff.outofgame.PlayerClasses
import models.bff.outofgame.gameconfig.PlayerStatus.{NotReady, Ready}
import models.bff.outofgame.gameconfig.{PlayerInfo, PlayerStatus}
import models.syntax.Pointed
import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.html.{Element, Select}
import services.localstorage.FLocalStorage
import utils.misc.{RGBAColour, RGBColour}
import services.localstorage._
import zio.clock.Clock
import zio.{CancelableFuture, UIO, ZIO, ZLayer}

/**
  * This component is displayed in the GameJoined component, and allows the player to select the game configuration
  * that concerns their entity.
  *
  * That means: their class, their colour...
  *
  * If the colour or class is not set in the `initialPlayerInfo` (mainly when a new player joins, not when they
  * refreshed the page), we search for the last used in the local storage. If we find one, and it is not expired, we
  * use these instead.
  *
  * @param initialPlayerInfo the initial player info as it is currently in the database. This is important because the
  *                          player can refresh their screen and need to have up to date information
  * @param playerInfoWriter each time the player makes a change to their character, the new information is sent to the
  *                         server through this [[com.raquo.airstream.core.Observer]], which ends up in the web socket.
  */
final class PlayerInfoOptionPanel private (initialPlayerInfo: PlayerInfo, playerInfoWriter: Observer[PlayerInfo])
    extends Component[html.Element] {

  val localStorage: ZLayer[Clock, Nothing, LocalStorage] = FLocalStorage.live

  def storeElement[A](key: String, element: A)(implicit encoder: Encoder[A]): CancelableFuture[Unit] =
    zio.Runtime.default.unsafeRunToFuture(storeAt(key, element).provideLayer(localStorage))
  def retrieveElement[A](key: String)(implicit decoder: Decoder[A]): ZIO[Clock, Nothing, Option[A]] =
    retrieveFrom[A](key).catchAll(_ => UIO.none).provideLayer(localStorage)

  /** Used to access the client bounded rect of the element. */
  private var maybeViewChild: Option[html.Element] = Option.empty

  def changeReadyState(ready: PlayerStatus): PlayerInfo => PlayerInfo      = _.copy(status = ready)
  def changeColour(colour: RGBColour): PlayerInfo => PlayerInfo            = _.copy(maybePlayerColour = Some(colour))
  def changeClass(playerClass: PlayerClasses): PlayerInfo => PlayerInfo    = _.copy(maybePlayerClass = Some(playerClass))
  def overridePlayerInfo(playerInfo: PlayerInfo): PlayerInfo => PlayerInfo = _ => playerInfo

  val changerBus: EventBus[PlayerInfo => PlayerInfo] = new EventBus
  val readyStateWriter: Observer[PlayerStatus]       = changerBus.writer.contramap(changeReadyState)
  val playerClassWriter: Observer[PlayerClasses]     = changerBus.writer.contramap(changeClass)
  val colourWriter: Observer[RGBAColour]             = changerBus.writer.contramap(changeColour).contramap(_.withoutAlpha)
  val overrideWriter: Observer[PlayerInfo]           = changerBus.writer.contramap(overridePlayerInfo)

  val $playerInfo: Signal[PlayerInfo] = changerBus.events.fold(initialPlayerInfo) { (info, changer) =>
    changer(info)
  }

  val playerClassStorageKey = "playerClass"

  def storePlayerClassChoice(playerClasses: PlayerClasses): CancelableFuture[Unit] =
    storeElement(playerClassStorageKey, playerClasses)

  /**
    * This is the selector for the Player class.
    * Choosing a different option goes to the playerClassWriter above
    */
  val classSelector: ReactiveHtmlElement[Select] = select(
    disabled <-- $playerInfo.map(_.isReady),
    PlayerClasses.allChoices.map(_.toString).map(cls => option(value := cls, cls)),
    inContext(
      elem =>
        onChange.mapTo(elem.ref.value).map(PlayerClasses.playerClassByName).collect {
          case Some(value) => value
        } --> playerClassWriter
    ),
    inContext(
      elem =>
        onChange.mapTo(elem.ref.value).map(PlayerClasses.playerClassByName).collect {
          case Some(value) => value
        } --> (cls => storePlayerClassChoice(cls))
    ),
    value <-- $playerInfo.changes.map(_.maybePlayerClass).collect { case Some(cls) => cls.toString }
  )

  /**
    * Part of the component to chose the colour.
    *
    * This is a simple rectangle on the screen, on which you can click. Clicking on it make the Colour picker appear.
    * Players can then chose a colour they like from the picker.
    */
  val pickerPositionBus: EventBus[(Double, Double)] = new EventBus
  val $maybePickerPosition: EventStream[(Option[(Double, Double)], PlayerInfo)] = EventStream
    .merge(
      pickerPositionBus.events.map(Some(_)),
      UnderModalLayer.closeModalEvents.mapTo(Option.empty[(Double, Double)])
    )
    .withCurrentValueOf($playerInfo)
    .filter(!_._2.isReady)
  val feedingPickerPosition =
    List(
      onClick
        .map(event => (event.clientX, event.clientY, maybeViewChild))
        .collect {
          case (x, y, Some(viewChild)) => (x, y, viewChild.getBoundingClientRect())
        }
        .map { case (x, y, rect) => (x - rect.left, y - rect.top) } --> pickerPositionBus,
      $maybePickerPosition.filter(_._1.isDefined)
        .mapTo(()) --> UnderModalLayer.showModalWriter
    )

  val playerColourStorageKey: String                               = "playerColour"
  def storePlayerColour(colour: RGBColour): CancelableFuture[Unit] = storeElement(playerColourStorageKey, colour)

  val colourSelector: ReactiveHtmlElement[html.Div] = div(
    "Choose a color: ",
    div(
      height := "30px",
      width := "50px",
      cursor <-- $playerInfo.map(_.isReady).map(if (_) "not-allowed" else "pointer"),
      backgroundColor <-- $playerInfo.map(_.maybePlayerColour.getOrElse(RGBColour.white).rgb),
      feedingPickerPosition
    ),
    child <-- $maybePickerPosition.map {
      case (maybePosition, maybeInfo) =>
        maybePosition.zip(Some(maybeInfo))
    }.map(_.map {
        case ((x, y), playerInfo) =>
          div(
            zIndex := "10",
            position := "absolute",
            left := s"${x + 10}px",
            top := s"${y + 10}px",
            reactChildInDiv(
              ColorPickerWrapper(
                colourWriter,
                playerInfo.maybePlayerColour.getOrElse(RGBColour.white).withAlpha(1.0)
              )
            )
          )
      })
      .map(_.getOrElse(emptyNode)),
    onMountCallback { ctx =>
      $playerInfo.changes
        .map(_.maybePlayerColour)
        .collect { case Some(colour) => colour }
        .foreach(storePlayerColour)(ctx.owner)
    }
  )

  def fillInitialInfoWithStorage(initialPlayerInfo: PlayerInfo): ZIO[Clock, Nothing, Unit] =
    for {
      colour <- ZIO
        .fromOption(initialPlayerInfo.maybePlayerColour)
        .catchAll(_ => retrieveElement[RGBColour](playerColourStorageKey).map(_.getOrElse(Pointed[RGBColour].unit)))
      playerClass <- ZIO.fromOption(initialPlayerInfo.maybePlayerClass).catchAll { _ =>
        retrieveElement[PlayerClasses](playerClassStorageKey).map(_.getOrElse(Pointed[PlayerClasses].unit))
      }
      playerInfo = initialPlayerInfo.copy(
        maybePlayerClass  = Some(playerClass),
        maybePlayerColour = Some(colour)
      )
      _ <- ZIO.effectTotal(overrideWriter.onNext(playerInfo))
    } yield ()

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
      onMountCallback(ctx => maybeViewChild = Some(ctx.thisNode.ref)),
      onMountCallback { _ =>
        zio.Runtime.default.unsafeRunToFuture(fillInitialInfoWithStorage(initialPlayerInfo))
      }
    )
}

object PlayerInfoOptionPanel {
  def apply(initialPlayerInfo: PlayerInfo, playerInfoWriter: Observer[PlayerInfo]): PlayerInfoOptionPanel =
    new PlayerInfoOptionPanel(initialPlayerInfo, playerInfoWriter)
}
