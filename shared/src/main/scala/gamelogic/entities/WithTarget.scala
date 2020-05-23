package gamelogic.entities

trait WithTarget extends Entity {

  /** Entity id of the current target. */
  def targetId: Entity.Id

  /**
    * Returns a copy of this [[WithTarget]] with an other target.
    *
    * @param newTargetId entity id of the new target.
    */
  def changeTarget(newTargetId: Entity.Id): WithTarget

}
