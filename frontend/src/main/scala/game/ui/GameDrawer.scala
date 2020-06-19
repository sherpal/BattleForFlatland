package game.ui

import assets.Asset
import com.raquo.airstream.core.Observer
import game.Camera
import game.ui.bossspecificdrawers.Boss102Drawer
import gamelogic.entities.boss.dawnoftime.Boss102
import gamelogic.entities.boss.{Boss101, BossEntity}
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.classes.pentagon.PentagonZone
import gamelogic.entities.movingstuff.PentagonBullet
import gamelogic.entities.staticstuff.Obstacle
import gamelogic.entities.{DummyMob, Entity, SimpleBulletBody}
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import org.scalajs.dom.html
import typings.pixiJs.PIXI.LoaderResource
import typings.pixiJs.mod._

import scala.collection.mutable

/**
  * This class is used to draw the game state at any moment in time.
  * The implementation is full of side effects, probably in the wrong fear of less good performance.
  */
final class GameDrawer(
    val application: Application,
    resources: PartialFunction[Asset, LoaderResource],
    bossStartPosition: Complex,
    startFightObserver: Observer[Unit]
) extends Drawer {

  @inline private def stage = application.stage

  val camera: Camera = new Camera(application.view.asInstanceOf[html.Canvas])

  val otherStuffContainerBelow: Container = new Container
  stage.addChild(otherStuffContainerBelow)
  val bulletContainer: Container = new Container
  stage.addChild(bulletContainer)
  val playerContainer: Container = new Container
  stage.addChild(playerContainer)
  val dummyMobContainer: Container = new Container
  stage.addChild(dummyMobContainer)
  val bossesContainer: Container = new Container
  stage.addChild(bossesContainer)
  val movingStuffContainer: Container = new Container
  stage.addChild(movingStuffContainer)
  val obstacleContainer: Container = new Container
  stage.addChild(obstacleContainer)
  val otherStuffContainerAbove: Container = new Container
  stage.addChild(otherStuffContainerAbove)

  private val startButton = new BossStartButton(bossStartPosition, resources, startFightObserver)
  stage.addChild(startButton.element)

  private val players: mutable.Map[Entity.Id, Sprite] = mutable.Map()

  private def drawPlayers(entities: List[PlayerClass]): Unit = {
    players.filterNot(playerInfo => entities.map(_.id).contains(playerInfo._1)).foreach {
      case (entityId, sprite) =>
        players -= entityId
        sprite.visible = false
    }

    entities.foreach { entity =>
      val sprite = players.getOrElse(entity.id, {
        val s = new Sprite(polygonTexture(entity.colour, 1.0, entity.shape))
        s.anchor.set(0.5, 0.5)
        players += (entity.id -> s)
        playerContainer.addChild(s)
        s
      })

      sprite.rotation = -entity.rotation
      camera.viewportManager(sprite, entity.pos, entity.shape.boundingBox)
    }
  }

  private val dummyMobSprites: mutable.Map[Entity.Id, Sprite] = mutable.Map.empty
  private def drawDummyMobs(entities: List[DummyMob], now: Long): Unit =
    entities.foreach { entity =>
      val sprite = dummyMobSprites.getOrElse(
        entity.id, {
          val s = new Sprite(polygonTexture(0xc0c0c0, 1.0, entity.shape))
          s.anchor.set(0.5, 0.5)
          dummyMobSprites += (entity.id -> s)
          dummyMobContainer.addChild(s)
          s
        }
      )

      sprite.rotation = -entity.rotation // orientation is reversed...
      camera.viewportManager(sprite, entity.currentPosition(now), entity.shape.boundingBox)
    }

  private val bossesSprites: mutable.Map[Entity.Id, Sprite]        = mutable.Map.empty
  private val bossesRangeIndicator: mutable.Map[Entity.Id, Sprite] = mutable.Map.empty
  private def drawBosses(entities: List[BossEntity], now: Long): Unit =
    entities.foreach { entity =>
      val sprite = bossesSprites.getOrElse(
        entity.id, {
          val s = new Sprite(diskTexture(0xFFFFFF, 1, entity.shape.radius, withBlackDot = true))
          s.anchor.set(0.5, 0.5)
          bossesSprites += (entity.id -> s)
          bossesContainer.addChild(s)
          s
        }
      )

      sprite.rotation = -entity.rotation
      camera.viewportManager(sprite, entity.pos, entity.shape.boundingBox)

      val rangeSprite = bossesRangeIndicator.getOrElse(
        entity.id, {
          val s = new Sprite(circleTexture(0xFFFFFF, 0.5, Boss101.meleeRange))
          s.anchor.set(0.5, 0.5)
          bossesRangeIndicator += (entity.id -> s)
          bossesContainer.addChild(s)
          s
        }
      )

      camera.viewportManager(rangeSprite, entity.pos, entity.shape.boundingBox)

    }

  private val pentagonBullets: mutable.Map[Entity.Id, Sprite] = mutable.Map.empty
  private def drawPentagonBullets(bullets: List[PentagonBullet], currentTime: Long): Unit = {
    // removing destroyed sprites
    pentagonBullets.filterNot(bullet => bullets.exists(_.id == bullet._1)).foreach {
      case (id, sprite) =>
        sprite.destroy()
        pentagonBullets -= id
    }

    // adding/moving bullets
    bullets.foreach { bullet =>
      val sprite = pentagonBullets.getOrElse(
        bullet.id, {
          val s = new Sprite(diskTexture(bullet.colour, 1, bullet.shape.radius))
          s.anchor.set(0.5, 0.5)
          pentagonBullets += (bullet.id -> s)
          movingStuffContainer.addChild(s)
          s
        }
      )
      camera.viewportManager(sprite, bullet.currentPosition(currentTime), bullet.shape.boundingBox)
    }
  }

  private val pentagonZones: mutable.Map[Entity.Id, Sprite] = mutable.Map.empty
  private def drawPentagonZones(zones: List[PentagonZone]): Unit = {
    pentagonZones.filterNot(zone => zones.exists(_.id == zone._1)).foreach {
      case (id, sprite) =>
        sprite.destroy()
        pentagonZones -= id
    }

    zones.foreach { zone =>
      val sprite = pentagonZones.getOrElse(
        zone.id, {
          val s = new Sprite(polygonTexture(zone.colour.intColour, zone.colour.alpha, zone.shape))
          s.anchor.set(0.5)
          pentagonZones += (zone.id -> s)
          otherStuffContainerBelow.addChild(s)
          s.rotation = -zone.rotation // zone rotations are fixed
          s
        }
      )
      camera.viewportManager(sprite, zone.pos, zone.shape.boundingBox)
    }
  }

  private val bulletSprites: mutable.Map[Entity.Id, Sprite] = mutable.Map()
  private def drawSimpleBullets(bullets: List[SimpleBulletBody], currentTime: Long): Unit =
    bullets.foreach { bullet =>
      val sprite = bulletSprites.getOrElse(bullet.id, {
        val s = new Sprite(diskTexture(0x000000, 1, bullet.shape.radius))
        s.anchor.set(0.5, 0.5)
        bulletSprites += (bullet.id -> s)
        bulletContainer.addChild(s)
        s
      })
      camera.viewportManager(sprite, bullet.currentPosition(currentTime), bullet.shape.boundingBox)
    }

  private val obstacleSprites: mutable.Map[Entity.Id, Sprite] = mutable.Map.empty
  private def drawObstacles(obstacles: List[Obstacle]): Unit = obstacles.foreach { obstacle =>
    val sprite = obstacleSprites.getOrElse(
      obstacle.id, {
        val s = new Sprite(polygonTexture(obstacle.colour.intColour, 1.0, obstacle.shape))
        s.anchor.set(0.5, 0.5)
        obstacleSprites += (obstacle.id -> s)
        obstacleContainer.addChild(s)
        s
      }
    )
    camera.viewportManager(sprite, obstacle.pos, obstacle.shape.boundingBox)
  }

  val boss102Drawer =
    new Boss102Drawer(
      application,
      resources,
      bossStartPosition,
      startFightObserver,
      camera,
      otherStuffContainerBelow,
      otherStuffContainerAbove
    )

  def drawGameState(gameState: GameState, cameraPosition: Complex, currentTime: Long): Unit = {
    camera.worldCenter = cameraPosition
    drawPlayers(gameState.players.values.toList)
    drawSimpleBullets(gameState.simpleBullets.values.toList, currentTime)
    drawDummyMobs(gameState.dummyMobs.values.toList, currentTime)
    drawBosses(gameState.bosses.valuesIterator.toList, currentTime)
    drawPentagonBullets(gameState.pentagonBullets.valuesIterator.toList, currentTime)
    drawObstacles(gameState.allObstacles.toList)
    drawPentagonZones(gameState.entities.valuesIterator.collect { case zone: PentagonZone => zone }.toList)

    gameState.bosses.valuesIterator
      .find(_.isInstanceOf[Boss102])
      .foreach(
        _ =>
          boss102Drawer.drawGameState(
            gameState,
            cameraPosition,
            currentTime
          )
      )

    startButton.update(gameState, camera)
  }

}
