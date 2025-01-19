package gamelogic.entities.classes

import gamelogic.entities.{LivingEntity, MovingBody, WithAbilities, WithName}
import gamelogic.physics.shape.Polygon
import utils.misc.RGBColour.fromIntColour
import models.bff.outofgame.PlayerClasses

trait PlayerClass(val cls: PlayerClasses)
    extends LivingEntity
    with MovingBody
    with WithAbilities
    with WithName {
  def colour: Int

  lazy val rgb: (Int, Int, Int) = {
    val c = fromIntColour(colour)
    (c.red, c.green, c.blue)
  }

  def shape: Polygon

  /** Players can always be stunned. */
  def canBeStunned: Boolean = true

}
