package game

import com.raquo.airstream.core.Observer
import com.raquo.airstream.signal.Signal
import com.raquo.laminar.api.L._
import game.ui.GameDrawer
import gamelogic.abilities.Ability
import gamelogic.abilities.hexagon.{FlashHeal, HexagonHot}
import gamelogic.abilities.pentagon.{CreatePentagonBullet, CreatePentagonZone, PentaDispel}
import gamelogic.abilities.square.{Cleave, Enrage, HammerHit, Taunt}
import gamelogic.abilities.triangle.{DirectHit, UpgradeDirectHit}
import gamelogic.entities.classes.pentagon.PentagonZone
import gamelogic.entities.{Entity, LivingEntity, MovingBody}
import gamelogic.gamestate.GameState
import gamelogic.gamestate.gameactions.EntityStartsCasting
import gamelogic.physics.Complex
import models.bff.ingame.{InGameWSProtocol, UserInput}
import org.scalajs.dom
import utils.misc.RGBColour
import gamelogic.abilities.triangle.Stun
import gamelogic.abilities.triangle.EnergyKick

/**
  * Singleton adding the effect of casting abilities.
  */
final class CastAbilitiesHandler(
    playerId: Entity.Id,
    userControls: UserControls,
    $gameStates: Signal[GameState],
    $maybeTarget: Signal[Option[MovingBody with LivingEntity]],
    $gameMousePosition: Signal[Complex],
    $strictGameStates: SignalViewer[GameState],
    socketOutWriter: Observer[InGameWSProtocol.Outgoing],
    choosingAbilityEffectPositionObserver: Observer[Option[Ability.AbilityId]],
    isChoosingAbilityEffectPosition: Signal[Option[Ability.AbilityId]],
    useAbilityEvents: EventStream[Ability.AbilityId],
    gameDrawer: GameDrawer,
    deltaTimeWithServer: Long,
    currentTime: () => Long
)(implicit owner: Owner) {

  private def serverTime = currentTime()

  userControls.$mouseClicks.withCurrentValueOf(isChoosingAbilityEffectPosition)
    .collect { case (event, Some(id)) => (event, id) }
    .withCurrentValueOf($gameStates)
    .foreach {
      case ((event, abilityId), gameState) =>
        val now = serverTime
        choosingAbilityEffectPositionObserver.onNext(None)
        val gamePosition = gameDrawer.camera.mousePosToWorld(userControls.effectiveMousePos(event))
        (abilityId, gameState.players.get(playerId)) match {
          case (_, None) =>
            dom.console.warn("You are dead")
          case (Ability.createPentagonZoneId, Some(me)) =>
            val ability = CreatePentagonZone(
              0L,
              now,
              playerId,
              gamePosition,
              PentagonZone.damageOnTick,
              0.0,
              RGBColour.fromIntColour(me.colour).withAlpha(0.5)
            )
            val action = EntityStartsCasting(0L, now, ability.castingTime, ability)
            if (!gameState.castingEntityInfo.isDefinedAt(playerId) && action
                  .isLegalDelay($strictGameStates.now(), deltaTimeWithServer + 100)) {
              socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(action :: Nil))
            } else if (scala.scalajs.LinkingInfo.developmentMode) {
              dom.console.warn("Can't cast CreatePentagonZone.")
            }
          case _ =>
            dom.console.error(s"I don't manage this ability id: $abilityId.")
        }
    }

  EventStream
    .merge(
      userControls.downInputs
        .collect { case abilityInput: UserInput.AbilityInput => abilityInput }
        .withCurrentValueOf($gameStates)
        .filter(_._2.players.isDefinedAt(playerId))
        .withCurrentValueOf($maybeTarget)
        .withCurrentValueOf($gameMousePosition)
        .map {
          case (((abilityInput, gameState), maybeTarget), worldMousePos) =>
            val me = gameState.players(playerId) // this is ok because of above filter

            (gameState, abilityInput.abilityId(me), maybeTarget, worldMousePos)
        },
      useAbilityEvents
        .withCurrentValueOf($gameStates)
        .withCurrentValueOf($maybeTarget)
        .withCurrentValueOf($gameMousePosition)
        .map {
          case (((abilityId, gameState), maybeTarget), worldMousePos) =>
            (gameState, Some(abilityId), maybeTarget, worldMousePos)
        }
    )
    .foreach {
      case (gameState, maybeAbilityId, maybeTarget, worldMousePos) =>
        maybeAbilityId.foreach { abilityId =>
          val now = serverTime
          abilityId match {
            case Ability.hexagonFlashHealId =>
              maybeTarget match {
                case None => dom.console.warn("You need to have a target to cast Flash heal.")
                case Some(target) =>
                  val ability = FlashHeal(0L, now, playerId, target.id)
                  val action  = EntityStartsCasting(0L, now, ability.castingTime, ability)
                  if (!gameState.castingEntityInfo.isDefinedAt(playerId) && action
                        .isLegalDelay($strictGameStates.now(), deltaTimeWithServer + 100)) {
                    socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(action :: Nil))
                  } else if (scala.scalajs.LinkingInfo.developmentMode) {
                    dom.console.warn("Can't cast FlashHeal.")
                  }
              }
            case Ability.hexagonHexagonHotId =>
              maybeTarget match {
                case None => dom.console.warn("You need a target to cast Hexagon Hot.")
                case Some(target) =>
                  val ability = HexagonHot(0L, now, playerId, target.id)
                  val action  = EntityStartsCasting(0L, now, ability.castingTime, ability)
                  if (!gameState.castingEntityInfo.isDefinedAt(playerId) && action
                        .isLegalDelay($strictGameStates.now(), deltaTimeWithServer + 100)) {
                    socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(action :: Nil))
                  } else if (scala.scalajs.LinkingInfo.developmentMode) {
                    dom.console.warn("Can't cast Hexagon Hot.")
                  }
              }
            case Ability.squareTauntId =>
              maybeTarget match {
                case None => dom.console.warn("You need a target to cast Square Taunt")
                case Some(target) =>
                  val ability = Taunt(0L, now, playerId, target.id)
                  val action  = EntityStartsCasting(0L, now, ability.castingTime, ability)

                  if (!gameState.castingEntityInfo.isDefinedAt(playerId) && action.isLegalDelay(
                        $strictGameStates.now(),
                        deltaTimeWithServer + 100
                      )) {
                    socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(action :: Nil))
                  } else {
                    dom.console.warn("Can't cast Taunt")
                  }
              }
            case Ability.squareHammerHit =>
              maybeTarget match {
                case None => dom.console.warn("You need a target to cast Square Hammer Hit")
                case Some(target) =>
                  val ability = HammerHit(0L, now, playerId, target.id)
                  val action  = EntityStartsCasting(0L, now, ability.castingTime, ability)
                  if (!gameState.castingEntityInfo.isDefinedAt(playerId) && action.isLegalDelay(
                        $strictGameStates.now(),
                        deltaTimeWithServer + 100
                      )) {
                    socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(action :: Nil))
                  } else {
                    dom.console.warn("Can't cast Taunt")
                  }
              }
            case Ability.squareCleaveId =>
              gameState.players.get(playerId) match {
                case Some(me) =>
                  val myPosition  = me.currentPosition(now)
                  val direction   = worldMousePos - myPosition
                  val startingPos = myPosition + me.shape.radius * direction.normalized
                  val ability = Cleave(
                    0L,
                    now,
                    playerId,
                    startingPos,
                    direction.arg
                  )
                  val action = EntityStartsCasting(0L, now, ability.castingTime, ability)
                  if (!gameState.entityIsCasting(playerId) && action.isLegalDelay(
                        $strictGameStates.now(),
                        deltaTimeWithServer + 100
                      )) {
                    socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(action :: Nil))
                  } else {
                    dom.console.warn("Can't use Cleave")
                  }
                case None =>
                  dom.console.warn("You are dead")
              }
            case Ability.triangleEnergyKick =>
              maybeTarget match {
                case None => dom.console.warn("You need a target to use energy kick!")
                case Some(target) =>
                  val ability = EnergyKick(0L, now, playerId, target.id)
                  val action  = EntityStartsCasting(0L, now, ability.castingTime, ability)
                  if (!gameState.entityIsCasting(playerId) && action.isLegalDelay(
                        $strictGameStates.now(),
                        deltaTimeWithServer + 100
                      )) {
                    socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(action :: Nil))
                  } else {
                    dom.console.warn("Can't use Energy Kick")
                  }
              }
            case Ability.triangleDirectHit =>
              maybeTarget match {
                case None => dom.console.warn("You need a target to cast Triangle Direct Hit")
                case Some(target) =>
                  val ability = DirectHit(0L, now, playerId, target.id, DirectHit.directHitDamage)
                  val action  = EntityStartsCasting(0L, now, ability.castingTime, ability)
                  if (!gameState.entityIsCasting(playerId) && action.isLegalDelay(
                        $strictGameStates.now(),
                        deltaTimeWithServer + 100
                      )) {
                    socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(action :: Nil))
                  } else {
                    dom.console.warn("Can't use DirectHit")
                  }
              }
            case Ability.triangleUpgradeDirectHit =>
              val ability = UpgradeDirectHit(0L, now, playerId)
              val action  = EntityStartsCasting(0L, now, ability.castingTime, ability)
              if (!gameState.entityIsCasting(playerId) && action.isLegalDelay(
                    $strictGameStates.now(),
                    deltaTimeWithServer + 100
                  )) {
                socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(action :: Nil))
              } else {
                dom.console.warn("Can't use UpgradeDirectHit")
              }
            case Ability.triangleStun =>
              maybeTarget match {
                case None => dom.console.warn("can't cast stun without target!")
                case Some(target) =>
                  val stun   = Stun(0L, now, playerId, target.id)
                  val action = EntityStartsCasting(0L, now, stun.castingTime, stun)
                  if (!gameState.entityIsCasting(playerId) && action.isLegalDelay(
                        $strictGameStates.now(),
                        deltaTimeWithServer + 100
                      )) {
                    socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(action :: Nil))
                  } else {
                    dom.console.warn("Can't use CreatePentagonBullet")
                  }
              }
            case Ability.pentagonPentagonBullet =>
              gameState.players.get(playerId) match {
                case Some(me) =>
                  val myPosition  = me.pos // should not be moving anyway
                  val direction   = worldMousePos - myPosition
                  val startingPos = myPosition + me.shape.radius * direction.normalized
                  val ability = CreatePentagonBullet(
                    0L,
                    now,
                    playerId,
                    startingPos,
                    CreatePentagonBullet.damage,
                    direction.arg,
                    me.colour
                  )
                  val action = EntityStartsCasting(0L, now, ability.castingTime, ability)
                  if (!gameState.entityIsCasting(playerId) && action.isLegalDelay(
                        $strictGameStates.now(),
                        deltaTimeWithServer + 100
                      )) {
                    socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(action :: Nil))
                  } else {
                    dom.console.warn("Can't use CreatePentagonBullet")
                  }
                case None =>
                  dom.console.warn("You are dead")
              }
            case Ability.pentagonDispelId =>
              maybeTarget match {
                case None => dom.console.warn("You need to have a target to cast Dispel.")
                case Some(target) =>
                  val ability = PentaDispel(0L, now, playerId, target.id)
                  val action  = EntityStartsCasting(0L, now, ability.castingTime, ability)
                  if (!gameState.castingEntityInfo.isDefinedAt(playerId) && action
                        .isLegalDelay($strictGameStates.now(), deltaTimeWithServer + 100)) {
                    socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(action :: Nil))
                  } else if (scala.scalajs.LinkingInfo.developmentMode) {
                    dom.console.warn("Can't cast Dispel.")
                  }
              }

            case Ability.createPentagonZoneId =>
              gameState.players
                .get(playerId)
                .foreach(
                  _ =>
                    choosingAbilityEffectPositionObserver.onNext(
                      Some(
                        Ability.createPentagonZoneId
                      )
                    )
                )

            case Ability.squareEnrageId =>
              gameState.players.get(playerId) match {
                case Some(_) =>
                  val ability = Enrage(0L, now, playerId)
                  val action  = EntityStartsCasting(0L, now, ability.castingTime, ability)
                  if (!gameState.entityIsCasting(playerId) && action.isLegalDelay(
                        $strictGameStates.now(),
                        deltaTimeWithServer + 100
                      )) {
                    socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(action :: Nil))
                  } else {
                    dom.console.warn("Can't use Enrage")
                  }
                case None => dom.console.warn("You are dead")
              }

            case _ =>
              // todo
              dom.console.warn(s"TODO: implement ability $abilityId")
          }
        }

    }

}
