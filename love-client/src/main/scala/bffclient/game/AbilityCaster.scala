package bffclient.game

import gamelogic.abilities.*
import gamelogic.abilities.hexagon.*
import gamelogic.abilities.pentagon.*
import gamelogic.abilities.square.*
import gamelogic.abilities.triangle.*
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.classes.hexagon.HexagonZone
import gamelogic.entities.classes.pentagon.PentagonZone
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.physics.Complex
import services.i18n.I18n
import utils.misc.{RGBAColour, RGBColour}

/** What to do after the player asked to use an ability. */
enum CastRequest {
  case Send(action: EntityStartsCasting)
  case ChoosePosition(abilityId: Ability.AbilityId)
  case Error(message: String)
}

/** Builds the abilities of the players, like the frontend's `CastAbilitiesHandler` (with the errors
  * in the language of `i18n`).
  */
final class AbilityCaster(myId: Entity.Id, i18n: I18n) {
  import CastRequest.*
  private given I18n = i18n

  /** The ability on key `index + 1` (abilities in the same order as the frontend's action bar). */
  def castByIndex(
      index: Int,
      gameState: GameState,
      now: Long,
      target: Option[Entity.Id],
      mouse: Complex
  ): CastRequest =
    gameState.players.get(myId) match {
      case None => Error(i18n.youAreDead)
      case Some(player) =>
        val abilities = player.abilities.toArray
        if index < abilities.length then
          cast(abilities(index), player, gameState, now, target, mouse)
        else Error(i18n.noAbilityOnKey(index + 1))
    }

  /** @param mouseOver
    *   the entity whose frame is under the mouse, if "mouse over" is enabled in the controls: the
    *   abilities with a target fall back to it when the target is missing or not valid for them
    */
  def cast(
      abilityId: Ability.AbilityId,
      player: PlayerClass,
      gameState: GameState,
      now: Long,
      target: Option[Entity.Id],
      mouse: Complex,
      mouseOver: => Option[Entity.Id] = None
  ): CastRequest =
    casts.get(abilityId) match {
      case Some(cast) => cast(CastContext(player, gameState, now, target, mouse, mouseOver))
      case None if placedCasts.contains(abilityId) => ChoosePosition(abilityId)
      case None => Error(i18n.abilityNotSupported(abilityId))
    }

  /** Zones are placed where the player clicks. */
  def castAtPosition(
      abilityId: Ability.AbilityId,
      position: Complex,
      gameState: GameState,
      now: Long
  ): CastRequest =
    gameState.players.get(myId) match {
      case None => Error(i18n.youAreDead)
      case Some(player) =>
        val colour = RGBColour.fromIntColour(player.colour).withAlpha(0.5)
        placedCasts.get(abilityId) match {
          case Some(ability) => send(ability(now, position, colour), gameState, now)
          case None => Error(i18n.abilityNotPlacedWithMouse(abilityId))
        }
    }

  /** What the abilities need to be built when the player asks to use them. */
  private final class CastContext(
      val player: PlayerClass,
      gameState: GameState,
      val now: Long,
      target: Option[Entity.Id],
      val mouse: Complex,
      mouseOver: => Option[Entity.Id]
  ) {
    def onTarget(ability: Entity.Id => Ability): CastRequest =
      withTarget(ability, target, mouseOver, gameState, now)

    def send(ability: Ability): CastRequest = AbilityCaster.this.send(ability, gameState, now)
  }

  /** How to cast each ability that is not placed with the mouse. */
  private val casts: Map[Ability.AbilityId, CastContext => CastRequest] = Map(
    Ability.hexagon.flashHealId -> (c => c.onTarget(FlashHeal(Ability.UseId.zero, c.now, myId, _))),
    Ability.hexagon.hexagonHotId -> (c =>
      c.onTarget(HexagonHot(Ability.UseId.zero, c.now, myId, _))
    ),
    Ability.square.tauntId     -> (c => c.onTarget(Taunt(Ability.UseId.zero, c.now, myId, _))),
    Ability.square.hammerHitId -> (c => c.onTarget(HammerHit(Ability.UseId.zero, c.now, myId, _))),
    Ability.square.cleaveId -> { c =>
      val myPosition = c.player.currentPosition(c.now)
      val direction  = c.mouse - myPosition
      c.send(
        Cleave(
          Ability.UseId.zero,
          c.now,
          myId,
          myPosition + c.player.shape.radius * direction.normalized,
          direction.arg
        )
      )
    },
    Ability.square.enrageId -> (c => c.send(Enrage(Ability.UseId.zero, c.now, myId))),
    Ability.square.guardId  -> (c => c.onTarget(Guard(Ability.UseId.zero, c.now, myId, _))),
    Ability.square.shieldSlamId -> (c =>
      c.onTarget(ShieldSlam(Ability.UseId.zero, c.now, myId, _))
    ),
    Ability.square.braceId -> (c => c.send(Brace(Ability.UseId.zero, c.now, myId))),
    Ability.triangle.energyKickId -> (c =>
      c.onTarget(EnergyKick(Ability.UseId.zero, c.now, myId, _))
    ),
    Ability.triangle.directHitId -> (c =>
      c.onTarget(DirectHit(Ability.UseId.zero, c.now, myId, _, DirectHit.directHitDamage))
    ),
    Ability.triangle.upgradeDirectHitId -> (c =>
      c.send(UpgradeDirectHit(Ability.UseId.zero, c.now, myId))
    ),
    Ability.triangle.stunId -> (c => c.onTarget(Stun(Ability.UseId.zero, c.now, myId, _))),
    Ability.triangle.cutId  -> (c => c.onTarget(Cut(Ability.UseId.zero, c.now, myId, _))),
    Ability.pentagon.pentagonBulletId -> { c =>
      val direction = Complex.rotation(c.player.rotation)
      c.send(
        CreatePentagonBullet(
          Ability.UseId.zero,
          c.now,
          myId,
          c.player.pos + c.player.shape.radius * direction,
          CreatePentagonBullet.damage,
          direction.arg,
          c.player.colour
        )
      )
    },
    Ability.pentagon.fractureId -> { c =>
      val direction = Complex.rotation(c.player.rotation)
      c.send(
        Fracture(
          Ability.UseId.zero,
          c.now,
          myId,
          c.player.pos + c.player.shape.radius * direction,
          Fracture.damage,
          direction.arg,
          c.player.colour
        )
      )
    },
    Ability.pentagon.pentagramId -> (c =>
      c.onTarget(Pentagram(Ability.UseId.zero, c.now, myId, _))
    ),
    Ability.pentagon.dispelId -> (c => c.onTarget(PentaDispel(Ability.UseId.zero, c.now, myId, _)))
  )

  /** The abilities placed with the mouse, from the time, the position and the colour of the zone.
    */
  private val placedCasts: Map[Ability.AbilityId, (Long, Complex, RGBAColour) => Ability] = Map(
    Ability.pentagon.pentagonZoneId -> ((now, position, colour) =>
      CreatePentagonZone(
        Ability.UseId.zero,
        now,
        myId,
        position,
        PentagonZone.damageOnTick,
        Angle.zero,
        colour
      )
    ),
    Ability.hexagon.hexagonZoneId -> ((now, position, colour) =>
      CreateHexagonZone(
        Ability.UseId.zero,
        now,
        myId,
        position,
        HexagonZone.healOnTick,
        Angle.zero,
        colour
      )
    )
  )

  /** On the target, or (like the frontend's `sendCastAbilityWithTarget`) on the mouse over entity
    * when there is no target or the ability cannot be cast on it.
    */
  private def withTarget(
      ability: Entity.Id => Ability,
      target: Option[Entity.Id],
      mouseOver: => Option[Entity.Id],
      gameState: GameState,
      now: Long
  ): CastRequest = {
    val onTarget =
      target.fold(Error(i18n.noTarget))(targetId => send(ability(targetId), gameState, now))
    onTarget match {
      case sent: Send => sent
      case failure =>
        mouseOver
          .filterNot(target.contains)
          .map(id => send(ability(id), gameState, now))
          .collect { case sent: Send => sent }
          .getOrElse(failure)
    }
  }

  private def send(ability: Ability, gameState: GameState, now: Long): CastRequest = {
    val action = EntityStartsCasting(GameAction.Id.zero, now, ability.castingTime, ability)
    action.isLegalDelay(gameState, 100).fold(Send(action))(Error(_))
  }
}
