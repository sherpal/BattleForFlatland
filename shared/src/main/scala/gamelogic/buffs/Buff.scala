package gamelogic.buffs

import gamelogic.entities.Entity

/**
  * A [[gamelogic.buffs.Buff]] is an effect that is tight to a particular entity, for a given amount of time.
  *
  * A buff can either set a passive effect on its bearer, or do stuff every now and then.
  */
trait Buff {

  /** Unique id of this buff during the game. */
  val buffId: Buff.Id

  /** Id of the entity at which the buff is attached. */
  val bearerId: Entity.Id

  /** Time (in millis) that the buff will last. */
  val duration: Long

  /** Game Time at which the buff appeared. */
  val appearanceTime: Long

  /**
    * Unique way to identify this buff from its source.
    *
    * This is only used by the frontend to know what icon to use.
    * This is hardcoded below and in the frontend Asset file.
    */
  def resourceIdentifier: Buff.ResourceIdentifier

}

object Buff {

  type Id = Long

  type ResourceIdentifier = Int

  final val hexagonHotIdentifier    = 1
  final val boss101BigDotIdentifier = 2

  final val squareDefaultShield = 3

}
