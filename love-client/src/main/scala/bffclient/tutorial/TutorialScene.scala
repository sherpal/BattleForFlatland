package bffclient.tutorial

import bffclient.hud.HudClient
import bffclient.love.Timer
import bffclient.render.{Painter, Smoothing}
import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss101.{BigDot, BigHit, SmallHit}
import gamelogic.abilities.hexagon.HexagonHot
import gamelogic.abilities.pentagon.{CreatePentagonZone, PentaDispel}
import gamelogic.abilities.square.{Brace, Guard, ShieldSlam, Taunt}
import gamelogic.abilities.triangle.{EnergyKick, UpgradeDirectHit}
import gamelogic.buffs.abilities.classes.{Facets, Fractured}
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.boss.Boss101
import gamelogic.entities.classes.pentagon.PentagonZone
import gamelogic.gameextras.FightLog
import gamelogic.gamestate.gameactions.*
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.{Complex, PosInt}
import gamelogic.utils.IdGeneratorContainer
import models.bff.ingame.Controls
import models.bff.outofgame.PlayerClasses
import services.i18n.I18n
import utils.misc.{RGBAColour, RGBColour}

/** A made-up moment of a fight against Boss 101, for the tutorial to show the HUD's frames with: a
  * real game state, made with the game's own actions (the boss halfway down, Big Dot on a Pentagon,
  * heals over time, the Square's shield, ...), where we play `myClass`.
  *
  * It is the [[HudClient]] of those frames: clicking a frame targets its entity, as in the game.
  * Time goes by in a loop of a few seconds (cooldowns, the boss casting Big Hit), from [[painter]].
  */
final class TutorialScene(
    myClass: PlayerClasses,
    myName: String,
    myColour: RGBColour,
    val controls: Controls,
    val i18n: I18n
) extends HudClient {
  import TutorialScene.*

  private given IdGeneratorContainer = IdGeneratorContainer.initialIdGeneratorContainer

  private def useId(): Ability.UseId = summon[IdGeneratorContainer].abilityUseId()
  private def actionId()             = summon[IdGeneratorContainer].actionId()

  private val bossId = summon[IdGeneratorContainer].entityId()

  /** The players of Boss 101's team (two Pentagons), one of them us, where they stand. */
  private final case class Member(id: Entity.Id, cls: PlayerClasses, name: String, at: Complex)

  private val team: Vector[Member] = {
    val classes = Vector(
      PlayerClasses.Square   -> Complex(0, -70),
      PlayerClasses.Hexagon  -> Complex(-120, -260),
      PlayerClasses.Triangle -> Complex(30, 70),
      PlayerClasses.Pentagon -> Complex(260, -180),
      PlayerClasses.Pentagon -> Complex(750, 260) // out of the Hexagon's range
    )
    val mine = classes.indexWhere(_._1 == myClass)
    // the others are named after their class: Pentagon, then Pentagon 2 (in our language)
    var named = Map.empty[PlayerClasses, Int]
    classes.zipWithIndex.map { case ((cls, at), i) =>
      val name =
        if i == mine then myName
        else {
          val count = named.getOrElse(cls, 0) + 1
          named += cls -> count
          val className = i18n.playerClassName(cls)
          if count == 1 then className else s"$className $count"
        }
      Member(summon[IdGeneratorContainer].entityId(), cls, name, at)
    }
  }

  private def member(cls: PlayerClasses, index: Int = 0): Entity.Id =
    team.filter(_.cls == cls)(index).id

  private val square    = member(PlayerClasses.Square)
  private val hexagon   = member(PlayerClasses.Hexagon)
  private val triangle  = member(PlayerClasses.Triangle)
  private val pentagon  = member(PlayerClasses.Pentagon)
  private val pentagon2 = member(PlayerClasses.Pentagon, 1)

  val myId: Option[Entity.Id] = team.find(_.cls == myClass).map(_.id)

  private var state = GameState.empty

  private def act(actions: GameAction*): Unit = state = state.applyActions(actions.toVector)

  /** The ability was used (its cooldown, its cost), without its effects. */
  private def use(ability: Ability): Unit =
    act(UseAbility(actionId(), ability.time, ability.casterId, ability.useId, ability))

  /** Like the game server: the ability is used, and has its effects. */
  private def cast(ability: Ability): Unit = {
    use(ability)
    act(ability.createActions(state)*)
  }

  // The fight started a minute ago
  private val palette = RGBColour.someColours.filterNot(_ == myColour)
  for (m, i) <- team.zipWithIndex do {
    val colour = if myId.contains(m.id) then myColour else palette(i % palette.size)
    act(AddPlayerByClass(actionId(), start, m.id, m.at, m.cls, colour.intColour, m.name))
    act(m.cls.builder.startingActions(start + 1, m.id)*)
  }
  act(SpawnBoss(actionId(), start + 5000, bossId, Boss101.name))

  /** What the players dealt to the boss, from its arrival until a moment ago. */
  private val damageDealt = Vector(
    triangle  -> 3100.0,
    pentagon  -> 2600.0,
    pentagon2 -> 2200.0,
    square    -> 1300.0
  )
  private val damageActions = damageDealt.flatMap((id, amount) =>
    Vector(
      EntityTakesDamage(actionId(), start + 6000, bossId, amount / 2, id, Ability.autoAttackId),
      EntityTakesDamage(actionId(), now - 500, bossId, amount / 2, id, Ability.autoAttackId)
    )
  )
  act(damageActions*)
  // the tank's abilities generate much more threat than their damage
  act(damageDealt.map { (id, amount) =>
    val threat = if id == square then 2.6 * amount else amount
    ThreatToEntityChange(actionId(), now - 500, bossId, id, ThreatAmount.fromValue(threat), true)
  }*)

  // What the boss did: its Big Hit comes back in a moment, Big Dot is on a Pentagon
  cast(BigDot(useId(), now - 14000, bossId, pentagon2))
  use(BigHit(useId(), now - BigHit.cooldown + 1500, bossId, square))
  cast(SmallHit(useId(), now - 1200, bossId, square, SmallHit.damageAmount))
  act(
    EntityTakesDamage(actionId(), now - 1000, square, 78, bossId, Ability.boss101SmallHitId),
    EntityTakesDamage(actionId(), now - 1000, triangle, 14, bossId, Ability.boss101SmallHitId),
    EntityTakesDamage(
      actionId(),
      now - 1000,
      pentagon2,
      4 * BigDot.damageOnTick,
      bossId,
      Ability.boss101BigDotId
    ),
    EntityTakesDamage(actionId(), now - 1000, hexagon, 6, bossId, Ability.boss101SmallHitId)
  )

  // What the players did lately
  for t <- Seq(9000, 7000, 5000, 2500) do cast(Brace(useId(), now - t, square))
  cast(Guard(useId(), now - 6000, square, pentagon2))
  cast(ShieldSlam(useId(), now - 3000, square, bossId))
  cast(Taunt(useId(), now - 400, square, bossId))
  cast(HexagonHot(useId(), now - 9000, hexagon, pentagon2))
  cast(HexagonHot(useId(), now - 1000, hexagon, square))
  cast(UpgradeDirectHit(useId(), now - 5000, triangle))
  cast(EnergyKick(useId(), now - 2500, triangle, bossId))
  cast(
    CreatePentagonZone(
      useId(),
      now - 4000,
      pentagon,
      Complex.zero,
      PentagonZone.damageOnTick,
      Angle.zero,
      RGBAColour(0, 0, 0, 0)
    )
  )
  cast(PentaDispel(useId(), now - 2000, pentagon, pentagon2))
  // the bullets hit, by the game server
  act(Fractured.refresh(state, bossId, pentagon, now - 3000)*)
  act(Facets.setStacks(state, pentagon, now - 1500, Some(PosInt.of(3)))*)

  // Their resources, at this moment
  for (id, ratio) <- Seq(
      square    -> 0.62,
      hexagon   -> 0.68,
      triangle  -> 0.45,
      pentagon  -> 0.7,
      pentagon2 -> 0.82
    )
  do
    for player <- state.players.get(id) do {
      val target = ratio * player.maxResourceAmount
      act(
        EntityResourceChanges(
          actionId(),
          now,
          id,
          target - player.resourceAmount.amount,
          player.resourceType
        )
      )
    }

  // A moment from now, the boss casts Big Hit on the tank
  act(
    EntityStartsCasting(
      actionId(),
      now + 2000,
      BigHit.castingTime,
      BigHit(useId(), now + 2000, bossId, square)
    )
  )

  val gameState: GameState = state.copy(time = now)

  /** The damage meter's, from the damage dealt (already in the game state). */
  val fightLog: FightLog = FightLog()
  fightLog.received(damageActions, gameState)

  /** The names of the team, sorted like the HUD's team frames. */
  def teamIds: Vector[Entity.Id] = team.sortBy(_.name).map(_.id)

  def boss: Entity.Id = bossId

  def idOf(cls: PlayerClasses, index: Int = 0): Entity.Id = member(cls, index)

  private var target = Option.empty[Entity.Id]

  /** The healer starts with the tank as target, the others with the boss. */
  def currentTarget: Option[Entity.Id] =
    target.orElse(Some(if myClass == PlayerClasses.Hexagon then square else bossId))
  def selectTarget(id: Entity.Id): Unit = target = Some(id)
  def lockInToTarget: Boolean           = false

  private val smoothing = Smoothing()

  /** The scene at this moment of its loop. */
  def painter: Painter = {
    val inLoop = (Timer.getTime() * 1000).toLong % loopDuration
    Painter(gameState, now + inLoop, Complex.zero, 1, 1, smoothing)
  }
}

object TutorialScene {
  private val start = 1000L

  /** The moment shown, a minute after the start. */
  private val now = 61000L

  private val loopDuration = 4000L
}
