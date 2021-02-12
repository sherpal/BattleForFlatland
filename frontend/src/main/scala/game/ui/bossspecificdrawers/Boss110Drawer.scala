package game.ui.bossspecificdrawers

import assets.Asset
import com.raquo.airstream.core.Observer
import game.Camera
import game.ui.Drawer
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss102.{BossHound, DamageZone}
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import typings.pixiJs.PIXI.{Container, LoaderResource}
import typings.pixiJs.mod.{Application, ParticleContainer, Sprite}
import typings.pixiJs.PIXI.DisplayObject
import utils.misc.RGBColour

import scala.collection.mutable
import gamelogic.entities.boss.boss110.BigGuy
import gamelogic.entities.boss.boss110.BombPod

final class Boss110Drawer(
    val application: Application,
    resources: PartialFunction[Asset, LoaderResource],
    bossStartPosition: Complex,
    startFightObserver: Observer[Unit],
    camera: Camera,
    otherStuffContainerBelow: Container,
    otherStuffContainerAbove: Container
) extends Drawer {

  def maybeEntityDisplayObjectById(entityId: Entity.Id): Option[DisplayObject] =
    bigGuiesSprites.get(entityId)

  private val bigGuyTexture = redimensionTexture(
    resources(Asset.ingame.gui.boss.dawnOfTime.boss110.bigGuy).texture,
    BigGuy.shape.radius * 2,
    BigGuy.shape.radius * 2
  )

  private val bombPodTexture = redimensionTexture(
    resources(Asset.ingame.gui.boss.dawnOfTime.boss110.bombPod).texture,
    BombPod.shape.radius * 2,
    BombPod.shape.radius * 2
  )

  private val bigGuyContainer = new typings.pixiJs.mod.Container
  otherStuffContainerBelow.addChild(bigGuyContainer)
  private val bombPodContainer = new typings.pixiJs.mod.Container
  otherStuffContainerBelow.addChild(bombPodContainer)

  private val bombPodSprites: mutable.Map[Entity.Id, Sprite] = mutable.Map.empty

  def drawBombPods(bombs: List[BombPod]): Unit = {
    val currentIds = bombs.map(_.id).toSet

    bombPodSprites
      .filterNot { case (entityId, _) => currentIds.contains(entityId) }
      .foreach {
        case (entityId, sprite) =>
          sprite.destroy()
          bombPodSprites -= entityId
      }

    bombs.foreach { bomb =>
      val sprite = bombPodSprites.getOrElse(bomb.id, {
        val s = new Sprite(bombPodTexture)
        s.anchor.set(0.5, 0.5)
        bombPodSprites += (bomb.id -> s)
        bombPodContainer.addChild(s)
        s
      })

      camera.viewportManager(sprite, bomb.pos, bomb.shape.boundingBox)
    }
  }

  private val bigGuiesSprites: mutable.Map[Entity.Id, Sprite] = mutable.Map.empty

  def drawBigGuies(bigGuies: List[BigGuy]): Unit = {
    val currentIds = bigGuies.map(_.id).toSet

    /** First removing dead [[BigGuy]]s */
    bigGuiesSprites
      .filterNot { case (entityId, _) => currentIds.contains(entityId) }
      .foreach {
        case (entityId, sprite) =>
          bigGuiesSprites -= entityId
          sprite.destroy()
      }

    /** Then updating remaining ones, creating missing ones along the way. */
    bigGuies.foreach { bigGuy =>
      val sprite = bigGuiesSprites.getOrElse(bigGuy.id, {
        val s = new Sprite(bigGuyTexture)
        s.anchor.set(0.5, 0.5)
        bigGuiesSprites += (bigGuy.id -> s)
        bigGuyContainer.addChild(s)
        s
      })

      sprite.rotation = -bigGuy.rotation
      camera.viewportManager(sprite, bigGuy.pos, bigGuy.shape.boundingBox)
    }
  }

  def drawGameState(gameState: GameState, cameraPosition: Complex, currentTime: Long): Unit = {
    drawBigGuies(gameState.allTEntities[BigGuy].values.toList)
    drawBombPods(gameState.allTEntities[BombPod].values.toList)
  }

}
