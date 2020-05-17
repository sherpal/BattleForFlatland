package gamelogic.entities.classes

import gamelogic.entities.{LivingEntity, MovingBody, WithAbilities}
import gamelogic.physics.shape.Polygon

trait PlayerClass extends LivingEntity with MovingBody with WithAbilities {
  def colour: Int

  def shape: Polygon
}
