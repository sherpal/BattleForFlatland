package game.ui.gui.components.buffs

import assets.Asset
import game.ui.gui.components.GUIComponent
import game.ui.gui.components.gridcontainer.GridContainer
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.LoaderResource

import scala.collection.mutable

final class BuffContainer(entityId: Entity.Id, resources: PartialFunction[Asset, LoaderResource]) extends GUIComponent {

  private val grid = new GridContainer[BuffIcon](GridContainer.Column, 10, 1)
  container.addChild(grid.container)

  private val currentBuffs: mutable.Set[BuffIcon] = mutable.Set.empty

  def update(gameState: GameState, currentTime: Long): Unit = {

    val buffsToBeRemoved = currentBuffs.filterNot(buff => gameState.buffById(buff.entityId, buff.entityId).isDefined)
    buffsToBeRemoved.foreach { buff =>
      currentBuffs -= buff
      grid.removeElement(buff)
    }

    val buffsToAdd = gameState
      .allBuffsOfEntity(entityId)
      .filterNot(
        buff =>
          currentBuffs.exists(
            buffIcon => buff.buffId == buffIcon.buffId
          )
      )

    buffsToAdd.foreach { buff =>
      val icon = new BuffIcon(entityId, buff.buffId, resources(Asset.buffAssetMap(buff.resourceIdentifier)).texture)
      currentBuffs += icon
      grid.addElement(icon)
    }

    grid.currentElements.foreach(_.update(gameState, currentTime))
  }

}
