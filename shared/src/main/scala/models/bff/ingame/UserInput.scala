package models.bff.ingame

import gamelogic.abilities.Ability
import gamelogic.entities.classes.PlayerClass
import gamelogic.physics.Complex
import models.bff.ingame.Controls.InputCode
import gamelogic.gameextras.GameMarker

/**
  * A [[UserInput]] corresponds to the input a user made, translated to something meaningful from the point of view
  * of the game.
  *
  * For example, the input of a direction, or using an ability.
  */
sealed trait UserInput {
  def isKnown: Boolean = true
}

object UserInput {

  sealed trait DirectionInput extends UserInput {
    def direction: Complex
  }
  case object Up extends DirectionInput {
    def direction: Complex = Complex.i
  }
  case object Down extends DirectionInput {
    def direction: Complex = -Complex.i
  }
  case object Right extends DirectionInput {
    def direction: Complex = 1
  }
  case object Left extends DirectionInput {
    def direction: Complex = -1
  }

  case object NextTarget extends UserInput

  /** Represent */
  case class AbilityInput(abilityIndex: Int) extends UserInput {
    def abilityId(player: PlayerClass): Option[Ability.AbilityId] =
      player.abilities.toList.drop(abilityIndex).headOption
  }

  case class GameMarkerInput(gameMarker: GameMarker, onTarget: Boolean) extends UserInput

  final val directions: List[DirectionInput] = List(Up, Down, Right, Left)

  /** If the key code from the event is not above, you can keep track with this unknown instance. */
  case class Unknown(code: InputCode) extends UserInput {
    override def isKnown: Boolean = false
  }

  def movingDirection(pressedInputs: Set[UserInput]): Complex =
    directions.filter(pressedInputs.contains).map(_.direction).sum.safeNormalized

}
