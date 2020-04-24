package gamelogic.entities

/** An entity is anything that exist in the game. */
trait Entity {

  val id: Entity.Id

}

object Entity {

  type Id = Long

}
