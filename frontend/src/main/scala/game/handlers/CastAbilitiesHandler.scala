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
import models.bff.ingame.Controls.InputCode
import models.bff.ingame.Controls.KeyCode
import models.bff.ingame.Controls.ModifiedKeyCode
import models.bff.ingame.Controls.MouseCode
import models.bff.ingame.Controls.KeyInputModifier.WithShift
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

class CastAbilitiesHandler(myId: Entity.Id, controls: Controls, deltaTimeWithServer: Long) {

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
      event: KeyboardEvent.KeyUp,
      context: SceneContext[InGameScene.StartupData],
      model: InGameScene.InGameModel,
      viewModel: IndigoViewModel,
      now: Long
  ): js.Array[CustomIndigoEvents] = {
    val gameState     = model.actionGatherer.currentGameState
    def worldMousePos = viewModel.worldMousePosition

    def maybeAbilityIndex = controls.abilityKeys.zipWithIndex.collectFirst {
      case (inputCode, index) if wasInputCode(inputCode, event, context.keyboard.keysDown) =>
        index
    }

    (for {
      abilityIndex <- maybeAbilityIndex
      player       <- maybeMe(gameState)
      abilities = player.abilities.toArray
      abilityId <- Option.when(abilityIndex < abilities.length)(abilities(abilityIndex))
    } yield {
      inline def maybeTarget = viewModel.maybeTarget
      abilityId match {
        case Ability.hexagonFlashHealId =>
          sendCastAbilityWithTarget(
            target => FlashHeal(Ability.UseId.zero, now, myId, target.id),
            maybeTarget,
            gameState,
            now
          )
        case Ability.hexagonHexagonHotId =>
          sendCastAbilityWithTarget(
            target => HexagonHot(Ability.UseId.zero, now, myId, target.id),
            maybeTarget,
            gameState,
            now
          )
        case Ability.squareTauntId =>
          sendCastAbilityWithTarget(
            target => Taunt(Ability.UseId.zero, now, myId, target.id),
            maybeTarget,
            gameState,
            now
          )

        case Ability.squareHammerHit =>
          sendCastAbilityWithTarget(
            target => HammerHit(Ability.UseId.zero, now, myId, target.id),
            maybeTarget,
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
            target => EnergyKick(Ability.UseId.zero, now, myId, target.id),
            maybeTarget,
            gameState,
            now
          )
        case Ability.triangleDirectHit =>
          sendCastAbilityWithTarget(
            target =>
              DirectHit(Ability.UseId.zero, now, myId, target.id, DirectHit.directHitDamage),
            maybeTarget,
            gameState,
            now
          )
        case Ability.triangleUpgradeDirectHit =>
          val ability = UpgradeDirectHit(Ability.UseId.zero, now, myId)
          sendCastAbility(ability, gameState, now)
        case Ability.triangleStun =>
          sendCastAbilityWithTarget(
            target => Stun(Ability.UseId.zero, now, myId, target.id),
            maybeTarget,
            gameState,
            now
          )
        case Ability.pentagonPentagonBullet =>
          val myPosition  = player.pos // should not be moving anyway
          val direction   = worldMousePos - myPosition
          val startingPos = myPosition + player.shape.radius * direction.normalized
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
            target => PentaDispel(Ability.UseId.zero, now, myId, target.id),
            maybeTarget,
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

  private def wasInputCode(
      inputCode: InputCode,
      event: KeyboardEvent,
      downKeys: Batch[Key]
  ): Boolean = inputCode match
    case kc: KeyCode =>
      kc.keyCode == event.keyCode.code && !downKeys.contains[Key](Key.SHIFT)
    case mkc @ ModifiedKeyCode(_, modifier) =>
      def isModifierDown = modifier match
        case WithShift => downKeys.contains[Key](Key.SHIFT)

      mkc.keyCode == event.keyCode.code && isModifierDown
    case MouseCode(_) => false

  def sendCastAbilityWithTarget(
      ability: Entity => Ability,
      maybeTarget: Option[Entity],
      gameState: GameState,
      time: Long
  ): js.Array[CustomIndigoEvents] =
    maybeTarget match {
      case None =>
        if (scala.scalajs.LinkingInfo.developmentMode) {
          dom.console.warn("No target")
        }
        js.Array(CustomIndigoEvents.GameEvent.ErrorMessage("No target"))
      case Some(target) => sendCastAbility(ability(target), gameState, time)
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
