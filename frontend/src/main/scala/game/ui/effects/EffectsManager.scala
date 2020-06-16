package game.ui.effects

import assets.Asset
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.Owner
import game.Camera
import game.ui.Drawer
import game.ui.effects.boss.boss102.{HoundLifeBarEffect, LivingDamageZoneEffect}
import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss102.PutLivingDamageZoneOnTarget
import gamelogic.abilities.square.Cleave
import gamelogic.buffs.boss.boss102.LivingDamageZone
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.boss102.{AddBossHound, PutLivingDamageZone}
import gamelogic.gamestate.gameactions.{EntityGetsHealed, EntityTakesDamage, UseAbility}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import typings.pixiJs.PIXI.LoaderResource
import typings.pixiJs.mod.{Application, Container, Graphics}
import utils.misc.RGBColour

import scala.collection.mutable

final class EffectsManager(
    playerId: Entity.Id,
    $actionsAndStates: EventStream[(GameAction, GameState)],
    camera: Camera,
    val application: Application,
    resources: PartialFunction[Asset, LoaderResource]
)(implicit owner: Owner)
    extends Drawer {

  private val container: Container = new Container
  application.stage.addChild(container)

  private val gameEffects: mutable.Set[GameEffect] = mutable.Set.empty

  $actionsAndStates.foreach {
    case (action, gameState) =>
      gameState
        .applyActionChangers(action)
        .flatMap {
          case EntityTakesDamage(_, time, entityId, amount, sourceId) if entityId == playerId && sourceId != playerId =>
            Some(
              new SimpleTextEffect(
                amount.toString,
                RGBColour.red,
                time,
                Path.goDown(2000, 40).jitter(math.Pi / 16) + gameState.players.get(playerId).fold(Complex.zero)(_.pos),
                camera
              )
            )
          case EntityTakesDamage(_, time, entityId, amount, sourceId) if sourceId == playerId =>
            Some(
              new SimpleTextEffect(
                amount.toString,
                RGBColour.white,
                time,
                Path.goUp(2000, 40).jitter(math.Pi / 16) + gameState
                  .movingBodyEntityById(entityId)
                  .fold(Complex.zero)(entity => entity.pos + entity.shape.radius * Complex.i),
                camera
              )
            )
          case EntityGetsHealed(_, time, entityId, amount, sourceId) if sourceId == playerId =>
            Some(
              new SimpleTextEffect(
                amount.toString,
                RGBColour.green,
                time,
                Path
                  .goUp(2000, 40)
                  .jitter(math.Pi / 16) + gameState.movingBodyEntityById(entityId).fold(Complex.zero)(_.pos),
                camera
              )
            )
          case UseAbility(_, time, casterId, _, ability: Cleave) =>
            gameState.players.get(casterId).map { player =>
              new FlashingShape(
                Cleave.cone,
                ability.position,
                ability.rotation,
                time,
                750L,
                camera,
                polygonTexture(player.colour, 0.9, Cleave.cone)
              )
            }
          case PutLivingDamageZone(_, time, buffId, bearerId, _, _) =>
            Some(
              new LivingDamageZoneEffect(
                bearerId,
                buffId,
                time,
                circleTexture(0xFF0000, 0.5, LivingDamageZone.range),
                camera,
                LivingDamageZone.range
              )
            )
          case AddBossHound(_, time, entityId, _) =>
            Some(
              new HoundLifeBarEffect(
                entityId,
                time,
                resources(Asset.ingame.gui.bars.minimalistBar).texture,
                resources(Asset.ingame.gui.bars.minimalistBar).texture,
                camera
              )
            )
          case _ =>
            Option.empty[SimpleTextEffect]
        }
        .foreach { effect =>
          effect.addToContainer(container)
          gameEffects += effect
        }

  }

  def update(currentTime: Long, gameState: GameState): Unit =
    gameEffects.foreach { effect =>
      if (effect.isOver(currentTime, gameState)) {
        gameEffects -= effect
        effect.destroy()
      } else {
        effect.update(currentTime, gameState)
      }
    }

}
