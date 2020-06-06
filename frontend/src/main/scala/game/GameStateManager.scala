package game

import assets.Asset
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.Owner
import com.raquo.airstream.signal.{Signal, SignalViewer}
import game.ui.GameDrawer
import game.ui.effects.EffectsManager
import game.ui.gui.GUIDrawer
import gamelogic.abilities.Ability
import gamelogic.abilities.hexagon.{FlashHeal, HexagonHot}
import gamelogic.abilities.pentagon.CreatePentagonBullet
import gamelogic.abilities.square.{HammerHit, Taunt}
import gamelogic.abilities.triangle.{DirectHit, UpgradeDirectHit}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.{Entity, LivingEntity, MovingBody}
import gamelogic.gamestate.gameactions.{EntityStartsCasting, MovingBodyMoves}
import gamelogic.gamestate.{AddAndRemoveActions, GameAction, GameState, ImmutableActionCollector}
import gamelogic.physics.Complex
import models.bff.ingame.{InGameWSProtocol, UserInput}
import org.scalajs.dom
import typings.pixiJs.PIXI.LoaderResource
import typings.pixiJs.mod.Application
import utils.pixi.monkeypatching.PIXIPatching._

import scala.scalajs.js.timers.setTimeout

final class GameStateManager(
    application: Application,
    initialGameState: GameState,
    $actionsFromServer: EventStream[AddAndRemoveActions],
    socketOutWriter: Observer[InGameWSProtocol.Outgoing],
    keyboard: Keyboard,
    mouse: Mouse,
    playerId: Entity.Id,
    deltaTimeWithServer: Long,
    resources: PartialFunction[Asset, LoaderResource],
    maybeTargetWriter: Observer[Option[Entity]]
)(implicit owner: Owner) {

  def serverTime: Long = System.currentTimeMillis() + deltaTimeWithServer

  val abilityCodes: List[String] = (1 to 9).map("Digit" + _).toList

  private var actionCollector = ImmutableActionCollector(initialGameState)
  private val gameDrawer      = new GameDrawer(application)

  private val gameStateBus: EventBus[GameState] = new EventBus[GameState]

  val $gameStates: Signal[GameState] = gameStateBus.events.startWith(initialGameState)
  private val $strictGameStates      = $gameStates.observe

  val $gameMousePosition: SignalViewer[Complex] = mouse.$effectiveMousePosition.map(gameDrawer.camera.mousePosToWorld)
    .startWith(Complex.zero)
    .observe
  val $mouseAngleWithPosition: SignalViewer[Angle] = $gameMousePosition.map { mousePosition =>
    val myPositionNow = $strictGameStates.now.players.get(playerId).fold(Complex.zero)(_.pos)
    (mousePosition - myPositionNow).arg
  }.observe

  val targetFromGUIBus = new EventBus[Entity.Id]

  /**
    * Signal containing the current target of the user, starting with no target.
    *
    * This target only has meaning inside the GUI (it is not encoded in the game state). However, it used when using
    * abilities involving targetting something.
    */
  val $maybeTarget: Signal[Option[MovingBody with LivingEntity]] =
    EventStream
      .merge(
        targetFromGUIBus.events.withCurrentValueOf($gameStates).map {
          case (id, gameState) => gameState.livingEntityAndMovingBodyById(id)
        },
        mouse.$mouseClicks.map(mouse.effectiveMousePos)
          .map(gameDrawer.camera.mousePosToWorld)
          .withCurrentValueOf($gameStates)
          .map {
            case (mousePosition, state) =>
              state.allTargetableEntities
                .find(entity => entity.shape.contains(mousePosition, entity.pos, entity.rotation))
          }
      )
      .startWith(Option.empty[MovingBody with LivingEntity])

  $maybeTarget.foreach(maybeTargetWriter.onNext)

  private var unconfirmedActions: List[GameAction] = Nil

  private def nextGameState(): Unit =
    gameStateBus.writer.onNext(actionCollector.currentGameState.applyActions(unconfirmedActions))

  $actionsFromServer.foreach {
    case AddAndRemoveActions(actionsToAdd, oldestTimeToRemove, idsOfActionsToRemove) =>
      actionCollector = actionCollector.slaveAddAndRemoveActions(actionsToAdd, oldestTimeToRemove, idsOfActionsToRemove)

      unconfirmedActions = unconfirmedActions.dropWhile(_.time < actionCollector.currentGameState.time)

      setTimeout(1) {
        actionsToAdd.filterNot(action => idsOfActionsToRemove.contains(action.id)).foreach(newActionsBus.writer.onNext)
      }

      nextGameState()
  }

  private val newActionsBus: EventBus[GameAction] = new EventBus[GameAction]

  val $actionsWithStates: EventStream[(GameAction, GameState)] = newActionsBus.events.withCurrentValueOf($gameStates)

  val effectsManager = new EffectsManager(playerId, $actionsWithStates, gameDrawer.camera, application)

  val pressedUserInputSignal: SignalViewer[Set[UserInput]] = keyboard.$pressedUserInput.observe

  /** Cast abilities */
  val useAbilityBus = new EventBus[Ability.AbilityId]
  EventStream
    .merge(
      keyboard.$downKeyEvents.filter(event => abilityCodes.contains(event.code))
        .withCurrentValueOf($gameStates)
        .filter(_._2.players.isDefinedAt(playerId))
        .withCurrentValueOf($maybeTarget)
        .withCurrentValueOf($gameMousePosition)
        .map {
          case (((event, gameState), maybeTarget), worldMousePos) =>
            val me = gameState.players(playerId) // this is ok because of above filter

            (gameState, abilityCodes.zip(me.abilities).find(_._1 == event.code), maybeTarget, worldMousePos)
        },
      useAbilityBus.events
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
        maybeAbilityId.foreach {
          case (_, abilityId) =>
            val now = serverTime
            abilityId match {
              case Ability.hexagonFlashHealId =>
                maybeTarget match {
                  case None => dom.console.warn("You need to have a target to cast Flash heal.")
                  case Some(target) =>
                    val ability = FlashHeal(0L, now, playerId, target.id)
                    val action  = EntityStartsCasting(0L, now, ability.castingTime, ability)
                    if (!gameState.castingEntityInfo.isDefinedAt(playerId) && action
                          .isLegalDelay($strictGameStates.now, deltaTimeWithServer + 100)) {
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
                          .isLegalDelay($strictGameStates.now, deltaTimeWithServer + 100)) {
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
                          $strictGameStates.now,
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
                          $strictGameStates.now,
                          deltaTimeWithServer + 100
                        )) {
                      socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(action :: Nil))
                    } else {
                      dom.console.warn("Can't cast Taunt")
                    }
                }
              case Ability.triangleDirectHit =>
                maybeTarget match {
                  case None => dom.console.warn("You need a target to cast Triangle Direct Hit")
                  case Some(target) =>
                    val ability = DirectHit(0L, now, playerId, target.id, DirectHit.directHitDamage)
                    val action  = EntityStartsCasting(0L, now, ability.castingTime, ability)
                    if (!gameState.entityIsCasting(playerId) && action.isLegalDelay(
                          $strictGameStates.now,
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
                      $strictGameStates.now,
                      deltaTimeWithServer + 100
                    )) {
                  socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(action :: Nil))
                } else {
                  dom.console.warn("Can't use UpgradeDirectHit")
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
                          $strictGameStates.now,
                          deltaTimeWithServer + 100
                        )) {
                      socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(action :: Nil))
                    } else {
                      dom.console.warn("Can't use CreatePentagonBullet")
                    }
                  case None =>
                    dom.console.warn("You are dead")
                }

              case _ =>
                // todo
                dom.console.warn(s"TODO: implement ability $abilityId")
            }
        }

    }

  /** After [[game.ui.GameDrawer]] so that gui is on top of the game. */
  private val guiDrawer =
    new GUIDrawer(
      playerId,
      application,
      resources,
      targetFromGUIBus.writer,
      $maybeTarget.observe,
      useAbilityBus.writer,
      gameDrawer.camera
    )

  var lastTimeStamp = 0L

  private val ticker = (_: Double) => {
    val pressedUserInput = pressedUserInputSignal.now

    val playerMovement = UserInput.movingDirection(pressedUserInput)

    val now       = serverTime
    val gameState = $strictGameStates.now
    val deltaTime = now - lastTimeStamp
    lastTimeStamp = now

    gameState.players.get(playerId) match {
      case Some(entity) =>
        val nextPos  = entity.pos + entity.speed * playerMovement * deltaTime / 1000
        val moving   = playerMovement != Complex.zero
        val rotation = $mouseAngleWithPosition.now

        if (moving || entity.moving != moving || rotation != entity.rotation) {
          val newAction = MovingBodyMoves(
            0L,
            now,
            playerId,
            nextPos,
            entity.direction,
            rotation,
            entity.speed,
            moving
          )
          unconfirmedActions = unconfirmedActions :+ newAction

          socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(List(newAction)))

          nextGameState()
        }
      case None =>
      // do nothing, player is dead
    }

    val gameStateToDraw = $strictGameStates.now
    gameDrawer.drawGameState(
      gameStateToDraw,
      gameStateToDraw.players
        .get(playerId)
        .map(_.pos)
        .orElse(gameStateToDraw.bosses.headOption.map(_._2.pos))
        .getOrElse(Complex.zero),
      now
    )
    guiDrawer.update(gameState, now)
    effectsManager.update(now)
  }

  application.ticker.add(ticker)

}
