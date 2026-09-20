package gamelogic.entities.boss.boss104

import gamelogic.entities.Entity.TeamId
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.{Body, Entity}
import gamelogic.physics.Complex
import gamelogic.physics.shape.ConvexPolygon

/** The triangle formed by the three [[BigGuyDeathMark]]s left by the big guys that Boss104 spawned.
  *
  * Besides materialising the zone itself, this entity is the little state machine of Boss104's
  * two-stage attack: it is created at [[DeathMarkTriangle.firstStage]] (being inside is deadly),
  * moves to [[DeathMarkTriangle.secondStage]] once
  * [[gamelogic.abilities.boss.boss104.DeathMarkTriangleAttack]] resolves for the first time (being
  * outside becomes deadly), and is removed when it resolves the second time. Both the boss AI and
  * the friendly bots simply read `stage` to know what to do, which keeps them stateless.
  *
  * @param pos
  *   centre of gravity of the three death marks
  * @param relativeVertices
  *   the three death mark positions, relative to `pos` and positively oriented (see
  *   [[DeathMarkTriangle.fromMarkPositions]], which is the only sane way to build this)
  */
final case class DeathMarkTriangle(
    id: Entity.Id,
    time: Long,
    pos: Complex,
    relativeVertices: Vector[Complex],
    stage: Int
) extends Body {

  override def rotation: Angle = 0

  override val shape: ConvexPolygon = ConvexPolygon(relativeVertices)

  override def teamId: TeamId = Entity.teams.mobTeam

  /** The three death mark positions, in world coordinates. */
  def vertices: Vector[Complex] = relativeVertices.map(_ + pos)

  /** During the first stage, players must be outside; during the second one, inside. */
  def insideIsDeadly: Boolean = stage == DeathMarkTriangle.firstStage

  def isLastStage: Boolean = stage == DeathMarkTriangle.secondStage

  def nextStage: DeathMarkTriangle = copy(stage = stage + 1)

  /** Damage that a player standing at that position takes when the attack resolves. */
  def damageAt(position: Complex): Double =
    if shape.contains(position, pos, rotation) == insideIsDeadly then
      DeathMarkTriangle.deadlyDamage
    else DeathMarkTriangle.safeDamage

}

object DeathMarkTriangle {

  inline def firstStage: Int  = 1
  inline def secondStage: Int = 2

  inline def deadlyDamage: Double = 60.0
  inline def safeDamage: Double   = 10.0

  /** Builds the triangle from the world positions of the three death marks.
    *
    * The vertices are re-centred on their centre of gravity, and sorted by argument around it: a
    * [[ConvexPolygon]] expects positively oriented vertices, and the death marks come in whatever
    * order the big guys happened to die.
    */
  def fromMarkPositions(
      id: Entity.Id,
      time: Long,
      markPositions: Vector[Complex]
  ): DeathMarkTriangle = {
    val centre = markPositions.sum / markPositions.length
    DeathMarkTriangle(id, time, centre, markPositions.map(_ - centre).sortBy(_.arg), firstStage)
  }

}
