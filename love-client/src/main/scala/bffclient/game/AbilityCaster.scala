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
import utils.misc.RGBColour

/** What to do after the player asked to use an ability. */
enum CastRequest {
  case Send(action: EntityStartsCasting)
  case ChoosePosition(abilityId: Ability.AbilityId)
  case Error(message: String)
}

/** Builds the abilities of the players, like the frontend's `CastAbilitiesHandler`. */
final class AbilityCaster(myId: Entity.Id) {
  import CastRequest.*

  /** The ability on key `index + 1` (abilities in the same order as the frontend's action bar). */
  def castByIndex(
      index: Int,
      gameState: GameState,
      now: Long,
      target: Option[Entity.Id],
      mouse: Complex
  ): CastRequest =
    gameState.players.get(myId) match {
      case None => Error("You are dead")
      case Some(player) =>
        val abilities = player.abilities.toArray
        if index < abilities.length then cast(abilities(index), player, gameState, now, target, mouse)
        else Error(s"No ability on key ${index + 1}")
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
      case Ability.pentagonDispelId =>
        onTarget(PentaDispel(Ability.UseId.zero, now, myId, _))
      case Ability.createPentagonZoneId | Ability.hexagonHexagonZoneId =>
        ChoosePosition(abilityId)
      case other =>
        Error(s"Ability $other is not supported yet")
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
      case None => Error("You are dead")
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
            Error(s"Ability $other is not placed with the mouse")
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
      target.fold(Error("No target"))(targetId => send(ability(targetId), gameState, now))
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
