package gamelogic.entities

/** Units whose radius can change. */
trait WithChangingRadius extends Entity {
  def radius: Double

  def changeRadius(newRadius: Double): WithChangingRadius
}
