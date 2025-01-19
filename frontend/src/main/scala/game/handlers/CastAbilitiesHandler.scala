package game.handlers

import indigo.*
import game.IndigoViewModel

import scala.scalajs.js
import game.events.CustomIndigoEvents
import game.scenes.ingame.InGameScene
import indigo.scenes.SceneContext
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import models.bff.ingame.Controls
import gamelogic.abilities.*

import org.scalajs.dom
import gamelogic.gamestate.gameactions.*
import gamelogic.gamestate.GameAction
import gamelogic.abilities.hexagon.*
import gamelogic.abilities.pentagon.*
import gamelogic.abilities.square.*
import gamelogic.abilities.triangle.*
import gamelogic.physics.Complex
import indigo.shared.events.MouseEvent.Click
import gamelogic.entities.classes.pentagon.PentagonZone
import utils.misc.RGBAColour
import utils.misc.RGBColour
import models.bff.ingame.UserInput.AbilityInput

class CastAbilitiesHandler(myId: Entity.Id, deltaTimeWithServer: Long) {

  def handleClickEvent(
      click: Click,
      viewModel: IndigoViewModel,
      now: => Long
  ): Outcome[IndigoViewModel] = viewModel.maybeChoosingAbilityPosition match {
    case None => Outcome(viewModel) // nothing to do here
    case Some(abilityId) =>
      maybeMe(viewModel.gameState) match {
        case Some(player) =>
          abilityId match {
            case Ability.createPentagonZoneId =>
              val events = sendCastAbility(
                CreatePentagonZone(
                  Ability.UseId.zero,
                  now,
                  myId,
                  viewModel.localMousePosToWorld(click.position),
                  PentagonZone.damageOnTick,
                  0,
                  RGBColour.fromIntColour(player.colour).withAlpha(0.5)
                ),
                viewModel.gameState,
                now
              )
              if events.exists {
                  case _: CustomIndigoEvents.GameEvent.SendAction => true
                  case _                                          => false
                }
              then Outcome(viewModel.stopChoosingAbilityPosition).addGlobalEvents(Batch(events))
              else Outcome(viewModel).addGlobalEvents(Batch(events))
            case id =>
              dom.console.warn(s"Weird, I should not be here with ability id $id")
              Outcome(viewModel)
          }
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
      abilityId match {
        case Ability.hexagonFlashHealId =>
          sendCastAbilityWithTarget(
            targetId => FlashHeal(Ability.UseId.zero, now, myId, targetId),
            maybeTargetId,
            gameState,
            now
          )
        case Ability.hexagonHexagonHotId =>
          sendCastAbilityWithTarget(
            targetId => HexagonHot(Ability.UseId.zero, now, myId, targetId),
            maybeTargetId,
            gameState,
            now
          )
        case Ability.squareTauntId =>
          sendCastAbilityWithTarget(
            targetId => Taunt(Ability.UseId.zero, now, myId, targetId),
            maybeTargetId,
            gameState,
            now
          )

        case Ability.squareHammerHit =>
          sendCastAbilityWithTarget(
            targetId => HammerHit(Ability.UseId.zero, now, myId, targetId),
            maybeTargetId,
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
            gameState,
            now
          )
        case Ability.triangleDirectHit =>
          sendCastAbilityWithTarget(
            targetId =>
              DirectHit(Ability.UseId.zero, now, myId, targetId, DirectHit.directHitDamage),
            maybeTargetId,
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
            gameState,
            now
          )
        case Ability.createPentagonZoneId =>
          js.Array(CustomIndigoEvents.GameEvent.StartChoosingAbility(Ability.createPentagonZoneId))

        case Ability.squareEnrageId =>
          sendCastAbility(Enrage(Ability.UseId.zero, now, myId), gameState, now)

        case _ =>
          // todo
          dom.console.warn(s"TODO: implement ability $abilityId")
          js.Array()
      }
    }).getOrElse(js.Array())
  }

  private def maybeMe(gameState: GameState) = gameState.players.get(myId)

  def sendCastAbilityWithTarget(
      ability: Entity.Id => Ability,
      maybeTargetId: Option[Entity.Id],
      gameState: GameState,
      time: Long
  ): js.Array[CustomIndigoEvents] =
    maybeTargetId match {
      case None =>
        if (scala.scalajs.LinkingInfo.developmentMode) {
          dom.console.warn("No target")
        }
        js.Array(CustomIndigoEvents.GameEvent.ErrorMessage("No target"))
      case Some(targetId) => sendCastAbility(ability(targetId), gameState, time)
    }

  def sendCastAbility(
      ability: Ability,
      gameState: GameState,
      time: Long
  ): js.Array[CustomIndigoEvents] = {
    val action = EntityStartsCasting(GameAction.Id.zero, time, ability.castingTime, ability)
    action.isLegalDelay(gameState, deltaTimeWithServer + 100) match {
      case Some(errorMessage) =>
        if (scala.scalajs.LinkingInfo.developmentMode) {
          dom.console.warn(errorMessage)
        }
        js.Array(CustomIndigoEvents.GameEvent.ErrorMessage(errorMessage))
      case None => js.Array(CustomIndigoEvents.GameEvent.SendAction(action))
    }
  }

}
