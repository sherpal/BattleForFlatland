package gamelogic.entities

/**
  * An [[ActionSource]] represents the kind thing that was responsible for an action.
  * [[ActionSource]] can be some physical or magical damage, but it can also be a healing power.
  */
sealed trait ActionSource

object ActionSource {

  case object Physical extends ActionSource
  case object Magical extends ActionSource

}
