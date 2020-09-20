package gamelogic.docs

/**
  * The [[BossMetadata]] describe the common metadata that boss entities must provide.
  */
trait BossMetadata {

  /**
    * Name of the boss (in English).
    *
    * This is a val so that we can pattern match on it.
    */
  val name: String

  def maxLife: Double

  /** Number of players this boss is intended to be fought against. */
  def intendedFor: Int

}
