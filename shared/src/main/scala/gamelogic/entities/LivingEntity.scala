package gamelogic.entities

/**
  * A [[gamelogic.entities.LivingEntity]] is an [[gamelogic.entities.Entity]] with a life count. It typically dies when
  * its life count goes to 0.
  */
trait LivingEntity extends Entity {

  /** Current health amount of the entity. */
  val life: Double

  /** Maximum amount of life this entity can have. */
  val maxLife: Double

  /** Update the life total by adding the specified delta, clamped to [O, maxLife]. */
  def changeLifeTotal(delta: Double): LivingEntity =
    patchLifeTotal((0.0 max (life + delta)) min maxLife)

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
