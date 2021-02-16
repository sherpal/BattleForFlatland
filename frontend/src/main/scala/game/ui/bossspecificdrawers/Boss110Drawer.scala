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
import gamelogic.entities.boss.boss110.{BigGuy, BombPod, SmallGuy}
import game.ui.EntitySpriteContainer
import gamelogic.entities.boss.boss110.CreepingShadow

import org.scalajs.dom

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
    bigGuiesDrawer
      .maybeSpriteById(entityId)
      .orElse(smallGuiesDrawer.maybeSpriteById(entityId))

  private val bigGuyTexture   = resources(Asset.ingame.gui.boss.dawnOfTime.boss110.bigGuy).texture
  private val smallGuyTexture = resources(Asset.ingame.gui.boss.dawnOfTime.boss110.smallGuy).texture
  private val bombPodTexture  = resources(Asset.ingame.gui.boss.dawnOfTime.boss110.bombPod).texture

  private val creepingShadowContainer = new typings.pixiJs.mod.Container
  otherStuffContainerBelow.addChild(creepingShadowContainer)
  private val bigGuyContainer = new typings.pixiJs.mod.Container
  otherStuffContainerBelow.addChild(bigGuyContainer)
  private val smallGuyContainer = new typings.pixiJs.mod.Container
  otherStuffContainerBelow.addChild(smallGuyContainer)
  private val bombPodContainer = new typings.pixiJs.mod.Container
  otherStuffContainerBelow.addChild(bombPodContainer)

  lazy val smallGuiesDrawer = new EntitySpriteContainer[SmallGuy](
    smallGuyContainer,
    smallGuyTexture,
    camera,
    SmallGuy.shape.radius * 2 / smallGuyTexture.width
  )
  lazy val bigGuiesDrawer = new EntitySpriteContainer[BigGuy](
    bigGuyContainer,
    bigGuyTexture,
    camera,
    BigGuy.shape.radius * 2 / bigGuyTexture.width
  )
  lazy val bombPodDrawer = new EntitySpriteContainer[BombPod](
    bombPodContainer,
    bombPodTexture,
    camera,
    BombPod.shape.radius * 2 / bombPodTexture.width
  )
  lazy val creepingShadowDrawer = new EntitySpriteContainer[CreepingShadow](
    creepingShadowContainer,
    diskTexture(RGBColour.black.intColour, 1, 5),
    camera
  )
  lazy val creepingShadowDrawer2 = new EntitySpriteContainer[CreepingShadow](
    creepingShadowContainer,
    diskTexture(RGBColour.black.intColour, 0.5, 30),
    camera
  )

  def drawGameState(gameState: GameState, cameraPosition: Complex, currentTime: Long): Unit = {
    bombPodDrawer.update(gameState, currentTime)
    bigGuiesDrawer.update(gameState, currentTime)
    smallGuiesDrawer.update(gameState, currentTime)
    creepingShadowDrawer.update(gameState, currentTime)
    creepingShadowDrawer2.update(gameState, currentTime)
  }

}
