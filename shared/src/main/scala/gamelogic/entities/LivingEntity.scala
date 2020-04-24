package gamelogic.entities

/**
  * A [[gamelogic.entities.LivingEntity]] is an [[gamelogic.entities.Entity]] with a life count. It typically dies when
  * its life count goes to 0.
  */
trait LivingEntity extends Entity {

  val life: Double

}
