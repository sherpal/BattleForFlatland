package gamelogic.entities
import gamelogic.physics.Complex
import gamelogic.physics.shape.{ConvexPolygon, Shape}

/**
  * This is a small mob for testing the ai manager design.
  * And perhaps make some tests in the future...
  */
final case class DummyMob(
    id: Entity.Id,
    time: Long,
    pos: Complex,
    speed: Double,
    moving: Boolean,
    direction: Double,
    rotation: Double
) extends LivingEntity
    with MovingBody {
  val life: Double    = 100.0
  val maxLife: Double = 100.0

  protected def patchLifeTotal(newLife: Double): DummyMob = this // this is dumb, but we kinda don't care.
  val shape: ConvexPolygon                                = DummyMob.shape
}

object DummyMob {

  val shape: ConvexPolygon = Shape.regularPolygon(3, 10)

  @inline final def speed: Double = 150.0

}
