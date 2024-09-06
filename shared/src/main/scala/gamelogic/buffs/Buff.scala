package gamelogic.buffs

import gamelogic.entities.Entity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.*

/** A [[gamelogic.buffs.Buff]] is an effect that is tight to a particular entity, for a given amount
  * of time.
  *
  * A buff can either set a passive effect on its bearer, or do stuff every now and then.
  */
trait Buff extends IdsProducer {

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

  /** Unique way to identify this buff from its source.
    *
    * This is only used by the frontend to know what icon to use. This is hardcoded below and in the
    * frontend Asset file.
    */
  def resourceIdentifier: Buff.ResourceIdentifier

  /** Actions that occur when the buff is removed.
    *
    * Example: adds a final heal at the end.
    */
  def endingAction(gameState: GameState, time: Long)(using IdGeneratorContainer): Vector[GameAction]

  /** Specifies whether this buff can be dispelled by player dispel abilities.
    *
    * By default this is false, but it can be overridden in concrete classes.
    */
  def canBeDispelled: Boolean = false

}

object Buff {

  opaque type Id = Long

  object Id extends OpaqueLongCompanion[Id]

  type ResourceIdentifier = Int

  private var lastId: ResourceIdentifier = 0
  def nextId(): ResourceIdentifier       = { lastId += 1; lastId }

  val hexagonHotIdentifier     = nextId()
  val boss101BigDotIdentifier  = nextId()
  val squareDefaultShield      = nextId()
  val rageFiller               = nextId()
  val healingThreatAware       = nextId()
  val damageThreatAware        = nextId()
  val energyFiller             = nextId()
  val triangleUpgradeDirectHit = nextId()
  val triangleStun             = nextId()
  val manaFiller               = nextId()
  val boss102DamageZoneBuff    = nextId()
  val squareEnrage             = nextId()
  val entitiesPentagonZoneBuff = nextId()
  val boss102LivingDamageZone  = nextId()
  val boss103Punished          = nextId()
  val boss103Purified          = nextId()
  val boss103Inflamed          = nextId()
  val boss110BrokenArmor       = nextId()

}
