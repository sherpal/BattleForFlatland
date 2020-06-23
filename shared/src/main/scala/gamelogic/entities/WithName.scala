package gamelogic.entities

/**
  * Any [[Entity]] which has a name.
  */
trait WithName extends Entity {

  def name: String

}
