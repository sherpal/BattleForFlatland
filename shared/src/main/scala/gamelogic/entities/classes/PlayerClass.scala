package gamelogic.entities.classes

import gamelogic.entities.{LivingEntity, MovingBody, WithAbilities, WithName}
import gamelogic.physics.shape.Polygon

trait PlayerClass extends LivingEntity with MovingBody with WithAbilities with WithName {
  def colour: Int

  def shape: Polygon
}
