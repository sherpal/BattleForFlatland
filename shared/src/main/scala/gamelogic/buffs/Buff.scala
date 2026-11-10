package gamelogic.buffs

import gamelogic.entities.Entity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.*
import io.circe.Codec
import io.circe.Encoder
import io.circe.Decoder
import boopickle.Pickler

import scala.collection.mutable

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

  /** Id of the entity that put the buff, or [[scala.None]] when it comes from the game itself (the
    * resources refilling, the zones ticking, the bosses' mechanics...).
    *
    * Every buff must say it, so that the frontend can show who put it (the border of its icon is in
    * the colour of that player). Buffs having a `sourceId` get it from [[WithSourceId]].
    */
  def maybeSourceId: Option[Entity.Id]

  /** Never ending buffs have their durations set to -1. */
  def isFinite: Boolean = duration >= 0L

  /** Unique way to identify this buff from its source.
    *
    * This is only used by the frontend to know what icon to use. This is hardcoded below and in the
    * frontend Asset file.
    */
  def resourceIdentifier: Buff.ResourceIdentifier

  /** Name of the buff (in English), shown to the players (when they hover its icon). By default,
    * the one given with its `resourceIdentifier`.
    */
  lazy val name: String = Buff.nameOf(resourceIdentifier)

  /** Actions that occur when the buff is removed.
    *
    * Example: adds a final heal at the end.
    *
    * If the spell disappears because it was dispelled, then ´maybeDispelledBy` will be filled by
    * the id of the entity who actually dispelled.
    */
  def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(using
      IdGeneratorContainer
  ): Vector[GameAction]

  /** Actions performed when the bearer of the buff died. By default, this is the same as the
    * `endingAction`. But you can override it to do something else.
    */
  def bearerDiedAction(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] = endingAction(gameState, time, maybeDispelledBy = Option.empty)

  /** Specifies whether this buff can be dispelled by player dispel abilities.
    *
    * By default this is false, but it can be overridden in concrete classes.
    */
  def canBeDispelled: Boolean = false

  /** The part of the duration that remains at `currentTime`, between 0 and 1. */
  final def remainingPercentage(currentTime: Long): Double = if isFinite then {
    val timeSinceAppeared = currentTime - appearanceTime
    val remainingTime     = duration - timeSinceAppeared
    val remainingPerc     = remainingTime.toDouble / duration
    remainingPerc max 0.0 min 1.0
  } else 1.0

}

//noinspection TypeAnnotation
object Buff {

  opaque type Id = Long

  object Id extends OpaqueLongCompanion[Id]

  opaque type ResourceIdentifier = Int

  opaque type StackedBuffIdentifier <: ResourceIdentifier = Int

  object ResourceIdentifier {
    given Codec[ResourceIdentifier] = Codec.from(Decoder.decodeInt, Encoder.encodeInt)

    given Pickler[ResourceIdentifier] = boopickle.Default.intPickler
  }

  object StackedBuffIdentifier {
    given Codec[StackedBuffIdentifier] = Codec.from(Decoder.decodeInt, Encoder.encodeInt)

    given Pickler[StackedBuffIdentifier] = boopickle.Default.intPickler
  }

  private var lastId: ResourceIdentifier = 0

  private val namesMap = mutable.Map.empty[ResourceIdentifier, String]

  private def nextId(name: String): ResourceIdentifier = {
    lastId += 1
    namesMap(lastId) = name
    lastId
  }
  private def nextStackedBuffId(name: String): StackedBuffIdentifier = nextId(name)

  // Each buff kind needs its own identifier here, with its name (in English) for the players. The
  // identifier is what the frontend uses to pick the icon, via `assets.Asset.buffAssetMap`.
  val hexagonHotIdentifier         = nextId("Heal Over Time")
  val boss101BigDotIdentifier      = nextId("Big Dot")
  val squareBrace                  = nextStackedBuffId("Brace")
  val rageFiller                   = nextId("Rage Filler")
  val healingThreatAware           = nextId("Healing Threat")
  val damageThreatAware            = nextId("Damage Threat")
  val energyFiller                 = nextId("Energy Filler")
  val triangleUpgradeDirectHit     = nextId("Upgrade Direct Hit")
  val triangleStun                 = nextId("Stunned")
  val silence                      = nextId("Silenced")
  val manaFiller                   = nextId("Mana Filler")
  val boss102DamageZoneBuff        = nextId("Damage Zone")
  val squareEnrage                 = nextId("Enrage")
  val entitiesPentagonZoneBuff     = nextId("Pentagon Zone")
  val entitiesHexagonZoneBuff      = nextId("Hexagon Zone")
  val boss102LivingDamageZone      = nextId("Living Damage Zone")
  val sirOfVicePunished            = nextId("Punished")
  val sirOfVicePurified            = nextId("Purified")
  val sirOfViceInflamed            = nextId("Inflamed")
  val boss110BrokenArmor           = nextId("Broken Armor")
  val boss104TwinDebuff            = nextId("Twin Debuff")
  val boss104BigGuyCurse           = nextId("Big Guy's Curse")
  val boss105TwinBond              = nextId("Twin Bond")
  val boss105ShadeBond             = nextId("Shade Bond")
  val boss105Mourning              = nextId("Mourning")
  val boss105VolatileCharge        = nextId("Volatile Charge")
  val boss105Yearning              = nextId("Yearning")
  val rageTrickle                  = nextId("Rage Trickle")
  val squareGuarding               = nextId("Guarding")
  val squareGuarded                = nextId("Guarded")
  val pentagonFacets               = nextStackedBuffId("Facets")
  val pentagonFractured            = nextId("Fractured")
  val spiritOfNaraChilled          = nextId("Chilled")
  val spiritOfNaraMassSlow         = nextId("Mass Slow")
  val spiritOfNaraPulled           = nextId("Pulled")
  val spiritOfNaraChained          = nextId("Chained")
  val spiritOfNaraCounterspelled   = nextId("Counterspelled")
  val spiritOfNaraPolymorphed      = nextId("Polymorphed")
  val spiritOfNaraDrinking         = nextId("Drinking")
  val spiritOfNaraFireLockout      = nextId("Fire Lockout")
  val spiritOfNaraFrostLockout     = nextId("Frost Lockout")
  val spiritOfNaraArcaneLockout    = nextId("Arcane Lockout")
  val spiritOfNaraFlameWreathWatch = nextId("Flame Wreath")
  val spiritOfNaraBlizzardTicks    = nextId("Blizzard")
  val spiritOfNaraEphemeral        = nextId("Ephemeral")

  private val names: Array[String] = {
    val arr = new Array[String](lastId)
    for (id, name) <- namesMap do arr(id - 1) = name
    arr
  }

  /** The name of the buffs of this kind, given with their identifier below. */
  def nameOf(identifier: ResourceIdentifier): String = names(identifier - 1)

}
