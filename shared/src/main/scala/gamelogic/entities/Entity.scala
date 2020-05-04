package gamelogic.entities

/** An entity is anything that exist in the game. */
trait Entity {

  val id: Entity.Id

  /** Time at which the entity was last modified. */
  val time: Long

}

object Entity {

  type Id = Long

}
