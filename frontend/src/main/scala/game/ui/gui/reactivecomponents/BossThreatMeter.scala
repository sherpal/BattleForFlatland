package game.ui.gui.reactivecomponents

import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.{Signal, Val}
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.Texture
import game.ui.reactivepixi.ChildrenReceiver.children
import game.ui.reactivepixi.ReactivePixiElement.ReactiveContainer
import gamelogic.entities.Entity.Id
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.classes.PlayerClass
import utils.misc.RGBColour
import game.ui.reactivepixi.AttributeModifierBuilder._
import gamelogic.physics.Complex

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

  val maybeMaxThreat: Signal[Option[ThreatAmount]] = threatsSignal.map(_.valuesIterator.maxOption)

  private def playerColourEvents(playerId: Entity.Id) =
    gameStateUpdates
      .map(_._1.entityByIdAs[PlayerClass](playerId))
      .collect { case Some(player) => player.colour }
      .toSignal(0)
      .map(RGBColour.fromIntColour)

  val barDimensions: Val[(Double, Double)] = Val((200.0, 15.0))

  val threatBarsSignal: Signal[List[ReactiveContainer]] = threatsSignal
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

        new StatusBar(
          percentages,
          playerColourEvents(entityId),
          Val(true),
          barTexture,
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
    children <-- threatBarsSignal
  )

}
