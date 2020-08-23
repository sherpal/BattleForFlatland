package gamelogic.buffs

import gamelogic.entities.Entity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/**
  * A [[gamelogic.buffs.Buff]] is an effect that is tight to a particular entity, for a given amount of time.
  *
  * A buff can either set a passive effect on its bearer, or do stuff every now and then.
  */
trait Buff {

  /** Unique id of this buff during the game. */
  def buffId: Buff.Id

  /** Id of the entity at which the buff is attached. */
  def bearerId: Entity.Id

  /** Time (in millis) that the buff will last. */
  def duration: Long

  /** Game Time at which the buff appeared. */
  def appearanceTime: Long

  /** Never ending buffs have their durations set to -1. */
  def isFinite: Boolean = duration >= 0L

  /**
    * Unique way to identify this buff from its source.
    *
    * This is only used by the frontend to know what icon to use.
    * This is hardcoded below and in the frontend Asset file.
    */
  def resourceIdentifier: Buff.ResourceIdentifier

  /**
    * Actions that occur when the buff is removed.
    *
    * Example: adds a final heal at the end.
    */
  def endingAction(gameState: GameState, time: Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction]

}

object Buff {

  type Id = Long

  type ResourceIdentifier = Int

  final val hexagonHotIdentifier     = 1
  final val boss101BigDotIdentifier  = 2
  final val squareDefaultShield      = 3
  final val rageFiller               = 4
  final val healingThreatAware       = 5
  final val damageThreatAware        = 6
  final val energyFiller             = 7
  final val triangleUpgradeDirectHit = 8
  final val manaFiller               = 9
  final val boss102DamageZoneBuff    = 10
  final val squareEnrage             = 11
  final val entitiesPentagonZoneBuff = 12
  final val boss102LivingDamageZone  = 13
  final val boss103Punished          = 14
  final val boss103Purified          = 15

}
