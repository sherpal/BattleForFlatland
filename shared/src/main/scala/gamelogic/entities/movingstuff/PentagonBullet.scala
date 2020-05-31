package gamelogic.entities.movingstuff

import gamelogic.entities.Entity.TeamId
import gamelogic.entities.{Entity, LivingEntity, MovingBody}
import gamelogic.entities.WithPosition.Angle
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Circle, Shape}
import utils.misc.RGBColour

/**
  * A [[PentagonBullet]] is spawned by the ability of the [[gamelogic.entities.classes.Pentagon]]. It moves on a strait
  * line for a given range until it hits an enemy. The hit enemy takes an amount of damage equal to the `damage`
  * property.
  */
final case class PentagonBullet(
    id: Entity.Id,
    time: Long,
    pos: Complex,
    speed: Double,
    direction: Angle,
    range: Double,
    damage: Double,
    ownerId: Entity.Id,
    teamId: TeamId,
    colour: Int
) extends MovingBody {
  def move(
      time: Long,
      position: Complex,
      direction: Angle,
      rotation: Angle,
      speed: Angle,
      moving: Boolean
  ): PentagonBullet =
    copy(time = time, pos = position, direction = direction, speed = speed)

  def shape: Circle = PentagonBullet.shape

  def rotation: Angle = 0.0

  def moving: Boolean = true

  def collideEnemy(gameState: GameState, currentTime: Long): Option[LivingEntity with MovingBody] =
    gameState.allLivingEntities
      .filter(entity => !gameState.areTheyFromSameTeam(entity.id, id).getOrElse(true))
      .find(_.collides(this, currentTime))

}

object PentagonBullet {

  final val shape = new Circle(4.0)

  final def defaultRange: Double = 1000
  final def defaultSpeed: Double = 300

}
