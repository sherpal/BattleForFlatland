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

}

object Buff {

  type Id = Long

}
