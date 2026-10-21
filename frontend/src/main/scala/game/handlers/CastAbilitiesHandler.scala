package game.handlers

import game.IndigoViewModel
import game.events.CustomIndigoEvents
import game.scenes.ingame.InGameScene
import gamelogic.abilities.*
import gamelogic.abilities.hexagon.*
import gamelogic.abilities.pentagon.*
import gamelogic.abilities.square.*
import gamelogic.abilities.triangle.*
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.classes.hexagon.HexagonZone
import gamelogic.entities.classes.pentagon.PentagonZone
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.gameactions.*
import gamelogic.physics.Complex
import indigo.*
import indigo.scenes.SceneContext
import indigo.shared.events.MouseEvent.Click
import models.bff.ingame.UserInput.AbilityInput
import org.scalajs.dom
import utils.misc.RGBColour

import scala.scalajs.js

class CastAbilitiesHandler(myId: Entity.Id) {

  def handleClickEvent(
      click: Click,
      viewModel: IndigoViewModel,
      now: => Long
  ): Outcome[IndigoViewModel] = viewModel.maybeChoosingAbilityPosition match {
    case None => Outcome(viewModel) // nothing to do here
    case Some(abilityId) =>
      maybeMe(viewModel.gameState) match {
        case Some(player) =>
          val events: js.Array[CustomIndigoEvents] = abilityId match {
            case Ability.createPentagonZoneId =>
              sendCastAbility(
                CreatePentagonZone(
                  Ability.UseId.zero,
                  now,
                  myId,
                  viewModel.localMousePosToWorld(click.position),
                  PentagonZone.damageOnTick,
                  Angle.zero,
                  RGBColour.fromIntColour(player.colour).withAlpha(0.5)
                ),
                viewModel.gameState,
                now
              )
            case Ability.hexagonHexagonZoneId =>
              sendCastAbility(
                CreateHexagonZone(
                  Ability.UseId.zero,
                  now,
                  myId,
                  viewModel.localMousePosToWorld(click.position),
                  HexagonZone.healOnTick,
                  Angle.zero,
                  RGBColour.fromIntColour(player.colour).withAlpha(0.5)
                ),
                viewModel.gameState,
                now
              )
            case id =>
              dom.console.warn(s"Weird, I should not be here with ability id $id")
              js.Array()
          }

          if events.exists {
              case _: CustomIndigoEvents.GameEvent.SendAction => true
              case _ => false
            }
          then Outcome(viewModel.stopChoosingAbilityPosition).addGlobalEvents(Batch(events))
          else Outcome(viewModel).addGlobalEvents(Batch(events))
        case None =>
          Outcome(viewModel)
      }

  }

  def handleKeyboardEvent(
      event: KeyboardHandler.RichKeyboardEvent[KeyboardEvent.KeyUp],
      context: SceneContext[InGameScene.StartupData],
      model: InGameScene.InGameModel,
      viewModel: IndigoViewModel,
      now: Long
  ): js.Array[CustomIndigoEvents] = {
    val gameState     = model.actionGatherer.currentGameState
    def worldMousePos = viewModel.worldMousePosition

    def maybeAbilityIndex = event.collectUserInput { case AbilityInput(abilityIndex) =>
      abilityIndex
    }

    (for {
      abilityIndex <- maybeAbilityIndex
      player       <- maybeMe(gameState)
      abilities = player.abilities.toArray
      abilityId <- Option.when(abilityIndex < abilities.length)(abilities(abilityIndex))
    } yield {
      inline def maybeTargetId = viewModel.maybeTargetId
      // only looked at when the current target is not valid for the ability
      def maybeMouseOverId: Option[Entity.Id] =
        if context.startUpData.controls.mouseOverEnabled then
          viewModel.uiParent.maybeEntityIdAt(
            context.frameContext,
            viewModel,
            context.mouse.position
          )
        else None
      abilityId match {
        case Ability.hexagonFlashHealId =>
          sendCastAbilityWithTarget(
            targetId => FlashHeal(Ability.UseId.zero, now, myId, targetId),
            maybeTargetId,
            maybeMouseOverId,
            gameState,
            now
          )
        case Ability.hexagonHexagonHotId =>
          sendCastAbilityWithTarget(
            targetId => HexagonHot(Ability.UseId.zero, now, myId, targetId),
            maybeTargetId,
            maybeMouseOverId,
            gameState,
            now
          )
        case Ability.squareTauntId =>
          sendCastAbilityWithTarget(
            targetId => Taunt(Ability.UseId.zero, now, myId, targetId),
            maybeTargetId,
            maybeMouseOverId,
            gameState,
            now
          )

        case Ability.squareHammerHit =>
          sendCastAbilityWithTarget(
            targetId => HammerHit(Ability.UseId.zero, now, myId, targetId),
            maybeTargetId,
            maybeMouseOverId,
            gameState,
            now
          )
        case Ability.squareCleaveId =>
          val myPosition  = player.currentPosition(now)
          val direction   = worldMousePos - myPosition
          val startingPos = myPosition + player.shape.radius * direction.normalized
          val ability = Cleave(
            Ability.UseId.zero,
            now,
            myId,
            startingPos,
            direction.arg
          )
          sendCastAbility(ability, gameState, now)

        case Ability.triangleEnergyKick =>
          sendCastAbilityWithTarget(
            targetId => EnergyKick(Ability.UseId.zero, now, myId, targetId),
            maybeTargetId,
            maybeMouseOverId,
            gameState,
            now
          )
        case Ability.triangleDirectHit =>
          sendCastAbilityWithTarget(
            targetId =>
              DirectHit(Ability.UseId.zero, now, myId, targetId, DirectHit.directHitDamage),
            maybeTargetId,
            maybeMouseOverId,
            gameState,
            now
          )
        case Ability.triangleUpgradeDirectHit =>
          val ability = UpgradeDirectHit(Ability.UseId.zero, now, myId)
          sendCastAbility(ability, gameState, now)
        case Ability.triangleStun =>
          sendCastAbilityWithTarget(
            targetId => Stun(Ability.UseId.zero, now, myId, targetId),
            maybeTargetId,
            maybeMouseOverId,
            gameState,
            now
          )
        case Ability.pentagonPentagonBullet =>
          val myPosition  = player.pos // should not be moving anyway
          val direction   = Complex.rotation(player.rotation)
          val startingPos = myPosition + player.shape.radius * direction
          val ability = CreatePentagonBullet(
            Ability.UseId.zero,
            now,
            myId,
            startingPos,
            CreatePentagonBullet.damage,
            direction.arg,
            player.colour
          )
          sendCastAbility(ability, gameState, now)
        case Ability.pentagonDispelId =>
          sendCastAbilityWithTarget(
            targetId => PentaDispel(Ability.UseId.zero, now, myId, targetId),
            maybeTargetId,
            maybeMouseOverId,
            gameState,
            now
          )
        case Ability.createPentagonZoneId =>
          js.Array(CustomIndigoEvents.GameEvent.StartChoosingAbility(Ability.createPentagonZoneId))

        case Ability.hexagonHexagonZoneId =>
          js.Array(CustomIndigoEvents.GameEvent.StartChoosingAbility(Ability.hexagonHexagonZoneId))

        case Ability.squareEnrageId =>
          sendCastAbility(Enrage(Ability.UseId.zero, now, myId), gameState, now)

        case Ability.triangleCut =>
          sendCastAbilityWithTarget(
            Cut(Ability.UseId.zero, now, myId, _),
            maybeTargetId,
            maybeMouseOverId,
            gameState,
            now
          )

        case _ =>
          // todo
          dom.console.warn(s"TODO: implement ability $abilityId")
          js.Array()
      }
    }).getOrElse(js.Array())
  }

  private def maybeMe(gameState: GameState) = gameState.players.get(myId)

  /** Casts the ability on the current target. If that is not possible (no target, or not a valid
    * one), falls back to the entity under the mouse, if the ability can legally be cast on it.
    */
  def sendCastAbilityWithTarget(
      ability: Entity.Id => Ability,
      maybeTargetId: Option[Entity.Id],
      maybeMouseOverId: => Option[Entity.Id],
      gameState: GameState,
      time: Long
  ): js.Array[CustomIndigoEvents] = {
    val targetAbilityOrError: Either[String, Ability] = maybeTargetId match {
      case None => Left("No target")
      case Some(targetId) =>
        val targetAbility = ability(targetId)
        illegalReason(targetAbility, gameState, time).toLeft(targetAbility)
    }

    targetAbilityOrError match {
      case Right(targetAbility) => sendCastAbility(targetAbility, gameState, time)
      case Left(errorMessage) =>
        maybeMouseOverId
          .filterNot(maybeTargetId.contains[Entity.Id])
          .map(ability)
          .filter(illegalReason(_, gameState, time).isEmpty)
          .fold(errorEvents(errorMessage))(sendCastAbility(_, gameState, time))
    }
  }

  def sendCastAbility(
      ability: Ability,
      gameState: GameState,
      time: Long
  ): js.Array[CustomIndigoEvents] =
    illegalReason(ability, gameState, time) match {
      case Some(errorMessage) => errorEvents(errorMessage)
      case None =>
        js.Array(CustomIndigoEvents.GameEvent.SendAction(entityStartsCasting(ability, time)))
    }

  private def entityStartsCasting(ability: Ability, time: Long) =
    EntityStartsCasting(GameAction.Id.zero, time, ability.castingTime, ability)

  /** The reason why this ability cannot be cast now, if it can't. */
  private def illegalReason(ability: Ability, gameState: GameState, time: Long): Option[String] =
    entityStartsCasting(ability, time).isLegalDelay(gameState, 100)

  private def errorEvents(errorMessage: String): js.Array[CustomIndigoEvents] = {
    if (scala.scalajs.LinkingInfo.developmentMode) {
      dom.console.warn(errorMessage)
    }
    js.Array(CustomIndigoEvents.GameEvent.ErrorMessage(errorMessage))
  }

}
