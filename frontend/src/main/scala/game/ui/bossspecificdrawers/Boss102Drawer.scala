package game.ui.bossspecificdrawers

import assets.Asset
import com.raquo.airstream.core.Observer
import game.Camera
import game.ui.Drawer
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss102.{BossHound, DamageZone}
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import typings.pixiJs.PIXI.LoaderResource
import typings.pixiJs.mod.{Application, Container, ParticleContainer, Sprite}
import utils.misc.RGBColour

import scala.collection.mutable

/**
  * Game drawer for the [[gamelogic.entities.boss.dawnoftime.Boss102]].
  *
  * Elements can be added either to the `otherStuffContainerBelow` or the `otherStuffContainerAbove`.
  * They only affect whether elements will be on top or below "default" game elements.
  */
final class Boss102Drawer(
    val application: Application,
    resources: PartialFunction[Asset, LoaderResource],
    bossStartPosition: Complex,
    startFightObserver: Observer[Unit],
    camera: Camera,
    otherStuffContainerBelow: Container,
    otherStuffContainerAbove: Container
) extends Drawer {

  private val damageZoneContainer = new ParticleContainer
  otherStuffContainerBelow.addChild(damageZoneContainer)
  private val boss102DamageZones: mutable.Map[Entity.Id, Sprite] = mutable.Map.empty
  private def drawBoss102DamageZones(damageZones: List[DamageZone]): Unit = {
    boss102DamageZones
      .collect { case (id, sprite) if !damageZones.map(_.id).contains(id) => (id, sprite) }
      .foreach {
        case (id, sprite) =>
          boss102DamageZones -= id
          sprite.destroy()
      }

    damageZones.foreach { zone =>
      val sprite = boss102DamageZones.getOrElse(
        zone.id, {
          val s = new Sprite(diskTexture(RGBColour.red.intColour, 0.5, zone.shape.radius))
          s.anchor.set(0.5, 0.5)
          boss102DamageZones += (zone.id -> s)
          damageZoneContainer.addChild(s)
          s
        }
      )
      camera.viewportManager(sprite, zone.pos, zone.shape.boundingBox)
    }
  }

  private val particleContainer = new ParticleContainer
  otherStuffContainerAbove.addChild(particleContainer)
  private val houndsSprites: mutable.Map[Entity.Id, Sprite] = mutable.Map.empty
  private def drawHounds(hounds: List[BossHound]): Unit = {
    houndsSprites
      .filter { case (id, _) => !hounds.map(_.id).contains(id) }
      .foreach {
        case (id, sprite) =>
          houndsSprites -= id
          sprite.destroy()
      }

    hounds.foreach { hound =>
      val sprite = houndsSprites.getOrElse(
        hound.id, {
          val s = new Sprite(polygonTexture(RGBColour.gray.intColour, 0.5, hound.shape))
          s.anchor.set(0.5, 0.5)
          houndsSprites += (hound.id -> s)
          particleContainer.addChild(s)
          s
        }
      )

      sprite.rotation = -hound.rotation
      camera.viewportManager(sprite, hound.pos, hound.shape.boundingBox)
    }
  }

  def drawGameState(gameState: GameState, cameraPosition: Complex, currentTime: Long): Unit = {
    drawBoss102DamageZones(gameState.otherEntities.valuesIterator.collect { case zone: DamageZone => zone }.toList)
    drawHounds(gameState.otherEntities.valuesIterator.collect { case hound: BossHound             => hound }.toList)
  }
}
