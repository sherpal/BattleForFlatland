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
import utils.misc.RGBColour

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
  ): CastRequest = {
    def onTarget(ability: Entity.Id => Ability) =
      withTarget(ability, target, mouseOver, gameState, now)
    abilityId match {
      case Ability.hexagonFlashHealId =>
        onTarget(FlashHeal(Ability.UseId.zero, now, myId, _))
      case Ability.hexagonHexagonHotId =>
        onTarget(HexagonHot(Ability.UseId.zero, now, myId, _))
      case Ability.squareTauntId =>
        onTarget(Taunt(Ability.UseId.zero, now, myId, _))
      case Ability.squareHammerHit =>
        onTarget(HammerHit(Ability.UseId.zero, now, myId, _))
      case Ability.squareCleaveId =>
        val myPosition = player.currentPosition(now)
        val direction  = mouse - myPosition
        send(
          Cleave(
            Ability.UseId.zero,
            now,
            myId,
            myPosition + player.shape.radius * direction.normalized,
            direction.arg
          ),
          gameState,
          now
        )
      case Ability.squareEnrageId =>
        send(Enrage(Ability.UseId.zero, now, myId), gameState, now)
      case Ability.squareGuardId =>
        onTarget(Guard(Ability.UseId.zero, now, myId, _))
      case Ability.squareShieldSlamId =>
        onTarget(ShieldSlam(Ability.UseId.zero, now, myId, _))
      case Ability.squareBraceId =>
        send(Brace(Ability.UseId.zero, now, myId), gameState, now)
      case Ability.triangleEnergyKick =>
        onTarget(EnergyKick(Ability.UseId.zero, now, myId, _))
      case Ability.triangleDirectHit =>
        onTarget(DirectHit(Ability.UseId.zero, now, myId, _, DirectHit.directHitDamage))
      case Ability.triangleUpgradeDirectHit =>
        send(UpgradeDirectHit(Ability.UseId.zero, now, myId), gameState, now)
      case Ability.triangleStun =>
        onTarget(Stun(Ability.UseId.zero, now, myId, _))
      case Ability.triangleCut =>
        onTarget(Cut(Ability.UseId.zero, now, myId, _))
      case Ability.pentagonPentagonBullet =>
        val direction = Complex.rotation(player.rotation)
        send(
          CreatePentagonBullet(
            Ability.UseId.zero,
            now,
            myId,
            player.pos + player.shape.radius * direction,
            CreatePentagonBullet.damage,
            direction.arg,
            player.colour
          ),
          gameState,
          now
        )
      case Ability.pentagonFractureId =>
        val direction = Complex.rotation(player.rotation)
        send(
          Fracture(
            Ability.UseId.zero,
            now,
            myId,
            player.pos + player.shape.radius * direction,
            Fracture.damage,
            direction.arg,
            player.colour
          ),
          gameState,
          now
        )
      case Ability.pentagonPentagramId =>
        onTarget(Pentagram(Ability.UseId.zero, now, myId, _))
      case Ability.pentagonDispelId =>
        onTarget(PentaDispel(Ability.UseId.zero, now, myId, _))
      case Ability.createPentagonZoneId | Ability.hexagonHexagonZoneId =>
        ChoosePosition(abilityId)
      case other =>
        Error(i18n.abilityNotSupported(other))
    }
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
        abilityId match {
          case Ability.createPentagonZoneId =>
            send(
              CreatePentagonZone(
                Ability.UseId.zero,
                now,
                myId,
                position,
                PentagonZone.damageOnTick,
                Angle.zero,
                colour
              ),
              gameState,
              now
            )
          case Ability.hexagonHexagonZoneId =>
            send(
              CreateHexagonZone(
                Ability.UseId.zero,
                now,
                myId,
                position,
                HexagonZone.healOnTick,
                Angle.zero,
                colour
              ),
              gameState,
              now
            )
          case other =>
            Error(i18n.abilityNotPlacedWithMouse(other))
        }
    }

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
