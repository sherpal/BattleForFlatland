package game.drawers.effects

import indigo.*
import scala.scalajs.js
import game.scenes.ingame.InGameScene.StartupData
import game.IndigoViewModel
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.*
import gamelogic.gamestate.gameactions.boss110.AddCreepingShadow
import gamelogic.abilities.boss.boss103.SacredGround
import gamelogic.abilities.boss.boss103.CleansingNova
import gamelogic.abilities.square.Cleave
import gamelogic.entities.Entity
import utils.misc.RGBColour
import gamelogic.physics.Complex
import scala.scalajs.js.JSConverters.*
import gamelogic.entities.classes.Constants
import assets.Asset

final class EffectsManager(currentEffects: js.Array[GameEffect], playerId: Entity.Id) {

  def present(context: FrameContext[StartupData], viewModel: IndigoViewModel): js.Array[SceneNode] =
    currentEffects.flatMap(_.present(context, viewModel))

  def update(context: FrameContext[StartupData], viewModel: IndigoViewModel): EffectsManager =
    EffectsManager(currentEffects.filterNot(_.isOver(context, viewModel)), playerId)

  def updateViewModel(
      context: FrameContext[StartupData],
      viewModel: IndigoViewModel
  ): IndigoViewModel = viewModel.copy(effectsManager = update(context, viewModel))

  def handleAction(
      action: GameAction,
      context: FrameContext[StartupData],
      viewModel: IndigoViewModel
  ): Option[EffectsManager] = {
    val gameState = viewModel.gameState

    val newEffects: js.Array[GameEffect] = gameState.applyActionChangers(action).toJSArray.flatMap {
      case EntityTakesDamage(_, time, entityId, amount, sourceId)
          if entityId == playerId && sourceId != playerId =>
        Some(
          SimpleTextEffect(
            amount.toInt.toString,
            "red",
            context.gameTime.running,
            Path
              .goDown(Seconds(2), 40)
              .jitter(math.Pi / 16) + gameState.players.get(playerId).fold(Complex.zero)(_.pos)
          )
        )
      case EntityTakesDamage(_, time, entityId, amount, sourceId) if sourceId == playerId =>
        Some(
          SimpleTextEffect(
            amount.toInt.toString,
            "white",
            context.gameTime.running,
            Path.goUp(Seconds(2), 40).jitter(math.Pi / 16) + gameState
              .movingBodyEntityById(entityId)
              .fold(Complex.zero)(entity => entity.pos + entity.shape.radius * Complex.i)
          )
        )
      case EntityGetsHealed(_, time, entityId, amount, sourceId) if sourceId == playerId =>
        Some(
          SimpleTextEffect(
            amount.toInt.toString,
            "green",
            context.gameTime.running,
            Path
              .goUp(Seconds(2), 40)
              .jitter(math.Pi / 16) + gameState
              .movingBodyEntityById(entityId)
              .fold(Complex.zero)(_.pos)
          )
        )
      case UseAbility(_, time, casterId, _, ability: Cleave) =>
        gameState.players.get(casterId).map { player =>
          FlashingShape(
            ability.position + player.shape.radius * Complex.rotation(ability.rotation),
            math.Pi + ability.rotation,
            context.gameTime.running,
            Millis(750L).toSeconds, {
              val cropSize = Size(135 / 3, 40)
              (0 until 3).toJSArray
                .map(index => Rectangle(Point(index * cropSize.width, 0), cropSize))
            },
            Asset.ingame.gui.abilities.cleaveEffect,
            Size(Cleave.cone.radius.toInt)
          )
        }
      case UseAbility(_, time, casterId, _, ability: gamelogic.abilities.triangle.DirectHit) =>
        for {
          target <- gameState.livingEntityAndMovingBodyById(ability.targetId)
          caster <- gameState.players.get(casterId)
          angle     = (caster.pos - target.pos).arg
          timeDelta = time - context.gameTime.running.toMillis.toLong
          path = -Path
            .arc(
              Millis(200).toSeconds,
              Constants.playerRadius * 1.5,
              angle - math.Pi / 4,
              angle + math.Pi / 4
            )
        } yield MovingSpriteEffect(
          Asset.ingame.gui.abilities.triangleDirectHitEffect,
          context.gameTime.running,
          path,
          timeDelta,
          (_, currentTime) => path(currentTime).arg,
          (gameState, currentTime) =>
            gameState.players.get(casterId).fold(Complex.zero)(_.currentPosition(currentTime))
        )
      // case PutLivingDamageZone(_, time, buffId, bearerId, _, _) =>
      //   Some(
      //     LivingDamageZoneEffect(
      //       bearerId,
      //       buffId,
      //       time,
      //       circleTexture(0xff0000, 0.5, LivingDamageZone.range),
      //       camera,
      //       LivingDamageZone.range
      //     )
      //   )
      // case AddBossHound(_, time, entityId, _) =>
      //   Some(
      //     HoundLifeBarEffect(
      //       entityId,
      //       time,
      //       resources(Asset.ingame.gui.bars.minimalistBar).texture,
      //       resources(Asset.ingame.gui.bars.minimalistBar).texture,
      //       camera
      //     )
      //   )
      // case UseAbility(_, time, casterId, _, _: CleansingNova) =>
      //   Some(
      //     CleansingNovaEffect(
      //       casterId,
      //       time,
      //       polygonTexture(
      //         RGBColour.gray.intColour,
      //         0.7,
      //         new ConvexPolygon(
      //           Vector(
      //             0.0,
      //             -3.i,
      //             10 - 3.i,
      //             10
      //           )
      //         )
      //       ),
      //       camera
      //     )
      //   )
      // case UseAbility(_, time, _, _, sacredGround: SacredGround) =>
      //   Some(
      //     SacredGroundEffect(
      //       sacredGround.position,
      //       sacredGround.radius,
      //       time,
      //       resources(sacredGroundArea).texture,
      //       camera
      //     )
      //   )
      // case _: EndGame if gameState.playersWon =>
      //   Some(new ConfettiEffect)
      // case addCreepingShadow: AddCreepingShadow =>
      //   Some(
      //     CreepingShadowArea(
      //       addCreepingShadow.entityId,
      //       addCreepingShadow.time,
      //       camera
      //     )
      //   )
      case _ =>
        Option.empty[GameEffect]
    }

    Option.when(newEffects.nonEmpty)(EffectsManager(currentEffects ++ newEffects, playerId))
  }

  def handleActionAndModifyViewModel(
      action: GameAction,
      context: FrameContext[StartupData],
      viewModel: IndigoViewModel
  ): IndigoViewModel = handleAction(action, context, viewModel) match {
    case None                    => viewModel
    case Some(newEffectsManager) => viewModel.copy(effectsManager = newEffectsManager)
  }

}

object EffectsManager {
  def empty(playerId: Entity.Id): EffectsManager = EffectsManager(js.Array(), playerId)
}
