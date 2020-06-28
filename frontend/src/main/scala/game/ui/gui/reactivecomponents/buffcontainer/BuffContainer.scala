package game.ui.gui.reactivecomponents.buffcontainer

import assets.Asset
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.Signal
import game.ui.gui.reactivecomponents.GUIComponent
import game.ui.gui.reactivecomponents.gridcontainer.GridContainer
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import game.ui.reactivepixi.ReactivePixiElement.ReactiveContainer
import gamelogic.physics.Complex
import typings.pixiJs.PIXI.{LoaderResource, Texture}
import game.ui.reactivepixi.AttributeModifierBuilder._

import utils.laminarzio.Implicits._
import scala.concurrent.duration._

final class BuffContainer(
    entityId: Entity.Id,
    resources: PartialFunction[Asset, LoaderResource],
    gameStateUpdates: EventStream[(GameState, Long)],
    iconSizeSignal: Signal[Double],
    positions: Signal[Complex]
) extends GUIComponent {

  val slowGameStateUpdates: EventStream[(GameState, Long)] = gameStateUpdates.spacedBy(500.millis)

  private val buffIcons: Signal[List[ReactiveContainer]] = gameStateUpdates
    .map(_._1)
    .map(_.allBuffsOfEntity(entityId).toList)
    .map(_.map(buff => (buff.buffId, buff.resourceIdentifier)))
    .toSignal(Nil)
    .split(_._1) {
      case (buffId, (_, identifier), _) =>
        new BuffIcon(
          entityId,
          buffId,
          resources(Asset.buffAssetMap(identifier)).texture,
          slowGameStateUpdates,
          iconSizeSignal
        )
    }
    .map(_.sortBy(_.buffId).map(x => x: ReactiveContainer))

  container.amend(
    new GridContainer[ReactiveContainer](
      GridContainer.Column,
      buffIcons,
      7
    ),
    position <-- positions
  )

}