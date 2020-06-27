package game.ui.gui.reactivecomponents.threatmeter

import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.{Signal, Val}
import game.ui.gui.reactivecomponents.GUIComponent
import game.ui.gui.reactivecomponents.gridcontainer.GridContainer
import game.ui.reactivepixi.AttributeModifierBuilder._
import game.ui.reactivepixi.ReactivePixiElement.ReactiveContainer
import gamelogic.entities.Entity
import gamelogic.entities.Entity.Id
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import typings.pixiJs.PIXI.Texture
import utils.misc.RGBColour

import scala.Ordering.Double.TotalOrdering

/**
  * The [[BossThreatMeter]] displays at the bottom right of the screen one bar per player who has damage
  * threat towards the boss.
  * The player with the highest threat amount is at the top, and other bars are shown relatively to that one.
  */
final class BossThreatMeter(
    bossId: Entity.Id,
    barTexture: Texture,
    gameStateUpdates: EventStream[(GameState, Long)],
    canvasSizeEvents: Signal[(Double, Double)]
) extends GUIComponent {

  val threatsSignal: Signal[Map[Id, ThreatAmount]] = gameStateUpdates
    .map(_._1)
    .map(_.entityByIdAs[BossEntity](bossId))
    .collect { case Some(boss) => boss }
    .map(_.damageThreats)
    .toSignal(Map())

  private val maybeMaxThreat = threatsSignal.map(_.valuesIterator.maxOption)

  private def playerEvents(playerId: Entity.Id) =
    gameStateUpdates
      .map(_._1.entityByIdAs[PlayerClass](playerId))
      .collect { case Some(player) => player }

  private val barDimensions = Val((200.0, 15.0))

  private val threatBarsSignal = threatsSignal
    .map(_.toList.sortBy(-_._2))
    .combineWith(maybeMaxThreat)
    .map {
      case (idsAndThreats, maybeMaxThreat) =>
        idsAndThreats.map((_, maybeMaxThreat))
    }
    .split(_._1._1) {
      case (entityId, _, threatsInfo) =>
        val percentages = threatsInfo
          .map {
            case ((_, amount), Some(maxAmount)) if maxAmount > 0 => amount / maxAmount
            case _                                               => 0.0
          }

        val playerInfo = playerEvents(entityId)

        new ThreatBar(
          playerInfo.map(_.name).startWith(""),
          barTexture,
          playerInfo.map(_.colour).startWith(0).map(RGBColour.fromIntColour),
          threatsInfo.map(_._1._2),
          percentages,
          barDimensions
        ): ReactiveContainer
    }

  container.amend(
    position <-- canvasSizeEvents
      .combineWith(threatBarsSignal.map(_.length))
      .combineWith(barDimensions)
      .map {
        case (((viewWidth, viewHeight), barNumber), (barWidth, barHeight)) =>
          Complex(viewWidth - barWidth, viewHeight - barNumber * barHeight)
      },
    new GridContainer[ReactiveContainer](
      GridContainer.Row,
      threatBarsSignal,
      10
    )
  )

}
