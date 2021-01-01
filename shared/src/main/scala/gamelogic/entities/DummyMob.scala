package gamelogic.entities
import gamelogic.entities.WithPosition.Angle
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
    direction: Angle,
    rotation: Angle
) extends LivingEntity
    with MovingBody {
  val life: Double    = 100.0
  val maxLife: Double = 100.0

  protected def patchLifeTotal(newLife: Double): DummyMob = this // this is dumb, but we kinda don't care.
  val shape: ConvexPolygon                                = DummyMob.shape

  def move(time: Long, position: Complex, direction: Angle, rotation: Angle, speed: Double, moving: Boolean): DummyMob =
    copy(time = time, pos = position, moving = moving, direction = direction, rotation = rotation)

  def teamId: Entity.TeamId = Entity.teams.mobTeam

  def canBeStunned: Boolean = true
}

object DummyMob {

  val shape: ConvexPolygon = Shape.regularPolygon(3, 10)

  @inline final def speed: Double = 150.0

}
