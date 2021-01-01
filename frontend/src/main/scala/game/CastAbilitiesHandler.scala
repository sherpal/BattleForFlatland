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
import game.ui.effects.errormessages.ErrorMessagesManager

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

  def sendCastAbilityWithTarget(
      ability: Entity => Ability,
      maybeTarget: Option[Entity],
      gameState: GameState,
      time: Long
  ): Unit =
    maybeTarget match {
      case None =>
        ErrorMessagesManager.logError("No target")
        if (scala.scalajs.LinkingInfo.developmentMode) {
          dom.console.warn("No target")
        }
      case Some(target) => sendCastAbility(ability(target), gameState, time)
    }

  def sendCastAbility(ability: Ability, gameState: GameState, time: Long): Unit = {
    val action = EntityStartsCasting(0L, time, ability.castingTime, ability)
    action.isLegalDelay(gameState, deltaTimeWithServer + 100) match {
      case Some(errorMessage) =>
        ErrorMessagesManager.logError(errorMessage)
        if (scala.scalajs.LinkingInfo.developmentMode) {
          dom.console.warn(errorMessage)
        }
      case None => socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(action :: Nil))
    }
  }

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
            sendCastAbility(ability, gameState, now)
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
              sendCastAbilityWithTarget(
                target => FlashHeal(0L, now, playerId, target.id),
                maybeTarget,
                gameState,
                now
              )
            case Ability.hexagonHexagonHotId =>
              sendCastAbilityWithTarget(
                target => HexagonHot(0L, now, playerId, target.id),
                maybeTarget,
                gameState,
                now
              )
            case Ability.squareTauntId =>
              sendCastAbilityWithTarget(
                target => Taunt(0L, now, playerId, target.id),
                maybeTarget,
                gameState,
                now
              )

            case Ability.squareHammerHit =>
              sendCastAbilityWithTarget(
                target => HammerHit(0L, now, playerId, target.id),
                maybeTarget,
                gameState,
                now
              )
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
                  sendCastAbility(ability, gameState, now)
                case None =>
                  ErrorMessagesManager.logError("You are dead")
                  if (scala.scalajs.LinkingInfo.developmentMode)
                    dom.console.warn("You are dead")
              }
            case Ability.triangleEnergyKick =>
              sendCastAbilityWithTarget(
                target => EnergyKick(0L, now, playerId, target.id),
                maybeTarget,
                gameState,
                now
              )
            case Ability.triangleDirectHit =>
              sendCastAbilityWithTarget(
                target => DirectHit(0L, now, playerId, target.id, DirectHit.directHitDamage),
                maybeTarget,
                gameState,
                now
              )
            case Ability.triangleUpgradeDirectHit =>
              val ability = UpgradeDirectHit(0L, now, playerId)
              sendCastAbility(ability, gameState, now)
            case Ability.triangleStun =>
              sendCastAbilityWithTarget(
                target => Stun(0L, now, playerId, target.id),
                maybeTarget,
                gameState,
                now
              )
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
                  sendCastAbility(ability, gameState, now)
                case None =>
                  ErrorMessagesManager.logError("You are dead")
                  if (scala.scalajs.LinkingInfo.developmentMode)
                    dom.console.warn("You are dead")
              }
            case Ability.pentagonDispelId =>
              sendCastAbilityWithTarget(
                target => PentaDispel(0L, now, playerId, target.id),
                maybeTarget,
                gameState,
                now
              )
            case Ability.createPentagonZoneId =>
              gameState.players
                .get(playerId)
                .foreach(
                  _ =>
                    choosingAbilityEffectPositionObserver.onNext(
                      Some(Ability.createPentagonZoneId)
                    )
                )

            case Ability.squareEnrageId =>
              sendCastAbility(Enrage(0L, now, playerId), gameState, now)

            case _ =>
              // todo
              dom.console.warn(s"TODO: implement ability $abilityId")
          }
        }

    }

}
