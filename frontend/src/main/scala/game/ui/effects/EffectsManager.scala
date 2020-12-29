package game.ui.effects

import assets.Asset
import assets.Asset.ingame.gui.boss.dawnOfTime.boss103.sacredGroundArea
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.Owner
import game.Camera
import game.ui.Drawer
import game.ui.effects.boss.boss102.{HoundLifeBarEffect, LivingDamageZoneEffect}
import game.ui.effects.boss.boss103.{CleansingNovaEffect, SacredGroundEffect}
import gamelogic.abilities
import gamelogic.abilities.boss.boss103.{CleansingNova, SacredGround}
import gamelogic.abilities.square.Cleave
import gamelogic.buffs.boss.boss102.LivingDamageZone
import gamelogic.entities.Entity
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.gameactions.boss102.{AddBossHound, PutLivingDamageZone}
import gamelogic.gamestate.gameactions.{EntityGetsHealed, EntityTakesDamage, UseAbility}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.shape.ConvexPolygon
import typings.pixiJs.PIXI.{LoaderResource, RenderTexture}
import typings.pixiJs.mod.{Application, Container, Graphics}
import typings.pixiJs.PIXI.DisplayObject
import utils.misc.RGBColour
import typings.pixiJs.PIXI.SCALE_MODES

import scala.collection.mutable

final class EffectsManager(
    playerId: Entity.Id,
    $actionsAndStates: EventStream[(GameAction, GameState)],
    camera: Camera,
    val application: Application,
    resources: PartialFunction[Asset, LoaderResource]
)(implicit owner: Owner)
    extends Drawer {

  import Complex.DoubleWithI

  val triangleHitTexture: RenderTexture = {
    val graphics = new Graphics()
      .beginFill(0)
      .drawRect(0, 0, 20, 2)
      .endFill()

    application.renderer.generateTexture(graphics, linearScale, 1)
  }

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
                amount.toInt.toString,
                RGBColour.red,
                time,
                Path.goDown(2000, 40).jitter(math.Pi / 16) + gameState.players.get(playerId).fold(Complex.zero)(_.pos),
                camera
              )
            )
          case EntityTakesDamage(_, time, entityId, amount, sourceId) if sourceId == playerId =>
            Some(
              new SimpleTextEffect(
                amount.toInt.toString,
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
                amount.toInt.toString,
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
          case UseAbility(_, time, casterId, _, ability: abilities.triangle.DirectHit) =>
            for {
              target <- gameState.livingEntityAndMovingBodyById(ability.targetId)
              caster <- gameState.players.get(casterId)
              angle = (caster.pos - target.pos).arg
              path  = -Path.arc(200, Constants.playerRadius * 1.5, angle - math.Pi / 4, angle + math.Pi / 4)
            } yield new MovingSpriteEffect(triangleHitTexture, time, path, (_, currentTime) => path(currentTime).arg, {
              (gameState, currentTime) =>
                gameState.players.get(casterId).fold(Complex.zero)(_.currentPosition(currentTime))
            }, camera)
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
          case UseAbility(_, time, casterId, _, _: CleansingNova) =>
            Some(new CleansingNovaEffect(casterId, time, {
              polygonTexture(
                RGBColour.gray.intColour,
                0.7,
                new ConvexPolygon(
                  Vector(
                    0.0,
                    -3.i,
                    10 - 3.i,
                    10
                  )
                )
              )
            }, camera))
          case UseAbility(_, time, _, _, sacredGround: SacredGround) =>
            Some(
              new SacredGroundEffect(
                sacredGround.position,
                sacredGround.radius,
                time,
                resources(sacredGroundArea).texture,
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

  def maybeEntityDisplayObjectById(entityId: Entity.Id): Option[DisplayObject] = None

}
