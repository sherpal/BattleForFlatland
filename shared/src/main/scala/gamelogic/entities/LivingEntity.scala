package gamelogic.entities

/**
  * A [[gamelogic.entities.LivingEntity]] is an [[gamelogic.entities.Entity]] with a life count. It typically dies when
  * its life count goes to 0.
  */
trait LivingEntity extends Entity {

  /** Current health amount of the entity. */
  val life: Double

  /** Maximum amount of life this entity can have. */
  def maxLife: Double

  /** Update the life total by adding the specified delta, clamped to [O, maxLife]. */
  def changeLifeTotal(delta: Double): LivingEntity =
    patchLifeTotal((0.0 max (life + delta)) min maxLife)

  /**
    * Boolean indicating whether this [[Entity]] is normally affected by "stun" ability.
    *
    * Stunning an [[Entity]] is a very powerful ability and therefore sometimes it can not be used,
    * otherwise it would be too powerful.
    * Typically, bosses are *not* affected by such mechanism.
    */
  def canBeStunned: Boolean

  /**
    * Copy this entity by only changing its life total.
    *
    * Note: this method is not supposed to take care about clamping the new life total to [0, maxLife], but must
    * simply patch the value as is.
    *
    * Typically, this will be implemented in concrete sub case classes as `copy(life = newLife)`.
    */
  protected def patchLifeTotal(newLife: Double): LivingEntity

}
