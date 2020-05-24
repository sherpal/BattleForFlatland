package gamelogic.entities.classes

import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction
import gamelogic.utils.IdGeneratorContainer

trait PlayerClassBuilder {

  /**
    * Actions to apply when the corresponding player is created.
    *
    * @param time time at which the actions take place
    * @param entityId id of the newly created entity
    * @param idGeneratorContainer id generator for actions requiring it.
    */
  def startingActions(time: Long, entityId: Entity.Id, idGeneratorContainer: IdGeneratorContainer): List[GameAction]

}
