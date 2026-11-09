package gamelogic.gameextras

import scala.collection.mutable

import gamelogic.abilities.Ability
import gamelogic.buffs.Buff
import gamelogic.buffs.abilities.classes.{BraceShield, Facets}
import gamelogic.entities.classes.{PlayerClass, Square}
import gamelogic.entities.{Entity, WithName}
import gamelogic.gamestate.gameactions.*
import gamelogic.gamestate.{GameAction, GameState}

/** What happened during the fight, for the damage meter and for the statistics shown at the end of
  * the game: the damage and the heals of everyone, by ability, the last moments of the players who
  * died, and a few things specific to the classes (the wasted facets of the pentagons, the brace of
  * the squares).
  *
  * Like the visual effects of the client, it is told about the actions received from the server,
  * and counts each one once, even if the server sends it again when it corrects the past. The
  * amounts counted are the ones that were really dealt, after the buffs of the target (like a
  * shield on the boss) transformed them.
  */
final class FightLog {
  import FightLog.*

  private val damage = mutable.Map.empty[Key, Totals]
  private val heals  = mutable.Map.empty[Key, Totals]

  /** What was dealt to one enemy (for the damage meter): the damage of each player, and when it was
    * first and last hit.
    */
  private final class Fight {
    val bySource: mutable.Map[Entity.Id, Double] = mutable.Map.empty
    var firstTime: Long                          = Long.MaxValue
    var lastTime: Long                           = Long.MinValue
  }

  private val fights = mutable.Map.empty[Entity.Id, Fight]

  /** The last life changes of each player (see `recapDuration`). */
  private val recent = mutable.Map.empty[Entity.Id, mutable.Queue[LifeChange]]

  private val deathsBuffer = mutable.ArrayBuffer.empty[Death]

  /** The names of the entities met, which may not exist anymore at the end. */
  private val names = mutable.Map.empty[Entity.Id, String]

  private val wastedFacets = mutable.Map.empty[Entity.Id, Int]

  /** For each pentagon, the facets lit so far by its bullets hitting at that time. */
  private val facetsLitAt = mutable.Map.empty[Entity.Id, (Long, Int)]

  /** For each living square (during the fight), its current brace stacks and since when. */
  private val currentBrace = mutable.Map.empty[Entity.Id, (Int, Long)]

  /** For each square, the time spent with each number of brace stacks (the index). */
  private val braceDurations = mutable.Map.empty[Entity.Id, Array[Long]]

  private val bracePrevented = mutable.Map.empty[Entity.Id, Double]

  private val seen      = mutable.Set.empty[GameAction.Id]
  private val seenOrder = mutable.Queue.empty[GameAction.Id]

  /** Applies the actions received from the server to `gameState`, recording what they did, and
    * returns the resulting game state.
    */
  def received(actions: Iterable[GameAction], gameState: GameState): GameState =
    actions.foldLeft(gameState) { (before, action) =>
      val after = action(before)
      record(action, before, after)
      after
    }

  private def firstTime(id: GameAction.Id): Boolean = seen.add(id) && {
    seenOrder.enqueue(id)
    if seenOrder.size > 5000 then seen.remove(seenOrder.dequeue())
    true
  }

  private def record(action: GameAction, before: GameState, after: GameState): Unit =
    action match {
      case damage: EntityTakesDamage if firstTime(damage.id) =>
        before.applyActionChangers(damage).foreach {
          case dealt: EntityTakesDamage => recordDamage(dealt, before, after)
          case _ => ()
        }
      case heal: EntityGetsHealed if firstTime(heal.id) =>
        before.applyActionChangers(heal).foreach {
          case healed: EntityGetsHealed => recordHeal(healed, before, after)
          case _ => ()
        }
      case RemoveEntity(_, time, entityId) =>
        before.entities.get(entityId).foreach {
          case player: PlayerClass => died(player.id, time)
          case _ => ()
        }
      case buff: PutStackedBuff if buff.resourceIdentifier == Buff.squareBrace =>
        braceChanged(buff.bearerId, buff.time, after)
      case RemoveBuff(_, time, bearerId, _) if currentBrace.contains(bearerId) =>
        braceChanged(bearerId, time, after)
      case GameStart(_, time) =>
        for case square: Square <- after.players.values do
          currentBrace(square.id) = (BraceShield.stacksOn(after, square.id).fold(0)(_.toInt), time)
      case EndGame(_, time) =>
        currentBrace.keys.toVector.foreach(closeBrace(_, time))
      case _ => ()
    }

  private def isPlayer(gameState: GameState, id: Entity.Id): Boolean =
    gameState.deadPlayers.contains(id)

  private def rememberName(gameState: GameState, id: Entity.Id): Unit =
    if !names.contains(id) then
      gameState.entities.get(id).foreach {
        case named: WithName => names(id) = named.name
        case _ => ()
      }

  private def add(totals: mutable.Map[Key, Totals], key: Key, amount: Double, effective: Double) =
    totals(key) = totals.getOrElse(key, Totals.zero).add(amount, effective)

  private def recordDamage(dealt: EntityTakesDamage, before: GameState, after: GameState): Unit = {
    val EntityTakesDamage(_, time, targetId, amount, sourceId, abilityId) = dealt
    if abilityId == Ability.pentagon.pentagonBulletId then litFacet(sourceId, time, before)
    for target <- before.livingEntityById(targetId) do {
      rememberName(before, sourceId)
      rememberName(before, targetId)
      val effective = math.min(amount, math.max(0.0, target.life))
      add(damage, Key(sourceId, targetId, abilityId), amount, effective)
      if isPlayer(before, sourceId) && !isPlayer(before, targetId) then {
        val fight = fights.getOrElseUpdate(targetId, new Fight)
        fight.bySource.update(sourceId, fight.bySource.getOrElse(sourceId, 0.0) + amount)
        fight.firstTime = math.min(fight.firstTime, time)
        fight.lastTime = math.max(fight.lastTime, time)
      }
      if isPlayer(before, targetId) then {
        val lifeAfter = after.livingEntityById(targetId).fold(0.0)(e => math.max(0.0, e.life))
        remember(
          targetId,
          LifeChange(time, sourceId, abilityId, amount, effective, false, lifeAfter)
        )
      }
      target match {
        case square: Square =>
          for stacks <- BraceShield.stacksOn(before, square.id) do
            bracePrevented(square.id) = bracePrevented.getOrElse(square.id, 0.0) +
              amount * (math.pow(BraceShield.damageMultiplier, -stacks.toInt) - 1)
        case _ => ()
      }
    }
  }

  private def recordHeal(healed: EntityGetsHealed, before: GameState, after: GameState): Unit = {
    val EntityGetsHealed(_, time, targetId, amount, sourceId, abilityId) = healed
    for target <- before.livingEntityById(targetId) do {
      rememberName(before, sourceId)
      rememberName(before, targetId)
      val effective = math.max(0.0, math.min(amount, target.maxLife - target.life))
      add(heals, Key(sourceId, targetId, abilityId), amount, effective)
      if isPlayer(before, targetId) then {
        val lifeAfter = after.livingEntityById(targetId).fold(0.0)(e => math.max(0.0, e.life))
        remember(
          targetId,
          LifeChange(time, sourceId, abilityId, amount, effective, true, lifeAfter)
        )
      }
    }
  }

  /** Like the server (see `ManagePentagonBullets`), the bullets of a pentagon hitting at the same
    * time light its facets together: the ones beyond `Facets.maxStacks` are wasted.
    */
  private def litFacet(pentagonId: Entity.Id, time: Long, before: GameState): Unit = {
    val stacks     = Facets.stacksOn(before, pentagonId).fold(0)(_.toInt)
    val alreadyLit = facetsLitAt.get(pentagonId).collect { case (`time`, lit) => lit }.getOrElse(0)
    facetsLitAt(pentagonId) = (time, alreadyLit + 1)
    if stacks + alreadyLit >= Facets.maxStacks.toInt then
      wastedFacets(pentagonId) = wastedFacets.getOrElse(pentagonId, 0) + 1
  }

  private def remember(playerId: Entity.Id, change: LifeChange): Unit = {
    val changes = recent.getOrElseUpdate(playerId, mutable.Queue.empty)
    changes.enqueue(change)
    while changes.headOption.exists(_.time < change.time - recapDuration) do changes.dequeue()
  }

  private def died(playerId: Entity.Id, time: Long): Unit = {
    deathsBuffer += Death(playerId, time, recapOf(playerId, time))
    recent.remove(playerId)
    closeBrace(playerId, time)
  }

  private def braceChanged(squareId: Entity.Id, time: Long, after: GameState): Unit =
    for (stacks, since) <- currentBrace.get(squareId) do {
      val newStacks = BraceShield.stacksOn(after, squareId).fold(0)(_.toInt)
      if newStacks != stacks then {
        closeBrace(squareId, time)
        currentBrace(squareId) = (newStacks, math.max(since, time))
      }
    }

  private def closeBrace(squareId: Entity.Id, time: Long): Unit =
    for (stacks, since) <- currentBrace.remove(squareId) do {
      val durations =
        braceDurations.getOrElseUpdate(squareId, new Array(BraceShield.maxStacks.toInt + 1))
      durations(stacks) += math.max(0L, time - since)
    }

  /** The damage dealt to `targetId` by each player, the highest first. */
  def damageTo(targetId: Entity.Id): Vector[(Entity.Id, Double)] =
    fights.get(targetId).fold(Vector.empty)(_.bySource.toVector.sortBy(-_._2))

  /** When `targetId` was first and last hit by a player. */
  def hitTimes(targetId: Entity.Id): Option[(Long, Long)] =
    fights.get(targetId).map(fight => (fight.firstTime, fight.lastTime))

  /** All the damage dealt, by source, target and ability. */
  def damageEntries: Vector[Entry] = damage.toVector.map((key, totals) => Entry(key, totals))

  /** All the heals done, by source, target and ability. */
  def healEntries: Vector[Entry] = heals.toVector.map((key, totals) => Entry(key, totals))

  /** The players who died, in the order of their deaths. */
  def deaths: Vector[Death] = deathsBuffer.toVector

  /** The life changes of `playerId` during the `recapDuration` before `time`: the ones before its
    * death if it died, otherwise its last ones.
    */
  def recapOf(playerId: Entity.Id, time: Long): Vector[LifeChange] =
    deathsBuffer
      .findLast(_.playerId == playerId)
      .fold(
        recent.get(playerId).fold(Vector.empty)(_.filter(_.time >= time - recapDuration).toVector)
      )(_.recap)

  /** The name of the entity, if it was ever met. */
  def nameOf(id: Entity.Id): Option[String] = names.get(id)

  /** The number of bullets of the pentagon that hit while its facets were all lit. */
  def facetsWastedBy(pentagonId: Entity.Id): Int = wastedFacets.getOrElse(pentagonId, 0)

  /** The time (in millis) that the square spent with each number of brace stacks (the index) during
    * the fight, while alive, until `now` if the fight is not over.
    */
  def braceTimeOf(squareId: Entity.Id, now: Long): Vector[Long] = {
    val durations = braceDurations
      .get(squareId)
      .fold(Vector.fill(BraceShield.maxStacks.toInt + 1)(0L))(
        _.toVector
      )
    currentBrace.get(squareId).fold(durations) { (stacks, since) =>
      durations.updated(stacks, durations(stacks) + math.max(0L, now - since))
    }
  }

  /** The damage that the brace of the square prevented. */
  def bracePreventedBy(squareId: Entity.Id): Double = bracePrevented.getOrElse(squareId, 0.0)

}

object FightLog {

  /** How long before a death (or before the end) the life changes of a player are kept. */
  inline def recapDuration: Long = 5000L

  final case class Key(sourceId: Entity.Id, targetId: Entity.Id, abilityId: Ability.AbilityId)

  /** @param amount
    *   what was dealt (or healed)
    * @param effective
    *   what the life of the target really lost (or gained): without overkill (or overheal)
    */
  final case class Totals(count: Int, amount: Double, effective: Double) {
    def add(more: Double, moreEffective: Double): Totals =
      Totals(count + 1, amount + more, effective + moreEffective)

    def +(that: Totals): Totals =
      Totals(count + that.count, amount + that.amount, effective + that.effective)
  }

  object Totals {
    val zero: Totals = Totals(0, 0.0, 0.0)
  }

  final case class Entry(key: Key, totals: Totals) {
    export key.*
  }

  /** A hit or a heal taken by a player, with its life afterwards. */
  final case class LifeChange(
      time: Long,
      sourceId: Entity.Id,
      abilityId: Ability.AbilityId,
      amount: Double,
      effective: Double,
      healing: Boolean,
      lifeAfter: Double
  )

  /** A player died at `time`, after the `recap` life changes. */
  final case class Death(playerId: Entity.Id, time: Long, recap: Vector[LifeChange])

}
