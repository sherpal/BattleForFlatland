package gamelogic.entities.classes

import gamelogic.abilities.Ability
import gamelogic.entities.{Entity, Resource}
import gamelogic.entities.Resource.ResourceAmount
import gamelogic.gamestate.GameAction
import gamelogic.physics.shape.{Polygon, Shape}
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

  /** The sets of all abilities available to this class. */
  def abilities: Set[Ability.AbilityId]

  /** The shape the class has. */
  def shape: Polygon

  /** "Normal" max life that the class has (could change with a buff, for example) */
  def initialMaxLife: Double

  /** The initial and "normal" max resource that the class has. */
  def initialResourceAmount: ResourceAmount

}
