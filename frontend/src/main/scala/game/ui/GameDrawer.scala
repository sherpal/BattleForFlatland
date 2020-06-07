package game.ui

import assets.Asset
import com.raquo.airstream.core.Observer
import game.Camera
import gamelogic.entities.boss.{Boss101, BossEntity}
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.movingstuff.PentagonBullet
import gamelogic.entities.staticstuff.Obstacle
import gamelogic.entities.{DummyMob, Entity, SimpleBulletBody}
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import org.scalajs.dom.html
import typings.pixiJs.PIXI
import typings.pixiJs.mod._
import typings.pixiJs.PIXI.LoaderResource

import scala.collection.mutable
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|

/**
  * This class is used to draw the game state at any moment in time.
  * The implementation is full of side effects, probably in the wrong fear of less good performance.
  */
final class GameDrawer(
    application: Application,
    resources: PartialFunction[Asset, LoaderResource],
    bossStartPosition: Complex,
    startFightObserver: Observer[Unit]
) {

  @inline private def stage = application.stage

  val camera: Camera = new Camera(application.view.asInstanceOf[html.Canvas])

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

  private def diskTexture(colour: Int, radius: Double, withBlackDot: Boolean = false): PIXI.RenderTexture = {
    val graphics = new Graphics
    graphics.lineStyle(0) // draw a circle, set the lineStyle to zero so the circle doesn't have an outline

    graphics.beginFill(colour, 1)
    graphics.drawCircle(0, 0, radius)
    graphics.endFill()

    if (withBlackDot) {
      graphics.beginFill(0x000000, 1)
      graphics.drawCircle(radius, 0.0, 3.0)
      graphics.endFill()
    }

    application.renderer.generateTexture(graphics, 1, 1)
  }

  private def circleTexture(colour: Int, alpha: Double, radius: Double): PIXI.RenderTexture = {
    val graphics = new Graphics
    graphics.lineStyle(1, colour, alpha)

    graphics.beginFill(0xFFFFFF, 0.0)
    graphics.drawCircle(0, 0, radius)

    application.renderer.generateTexture(graphics, 1, 1)

  }

  private def polygonTexture(colour: Int, shape: gamelogic.physics.shape.Polygon): PIXI.RenderTexture = {
    val graphics = new Graphics
    graphics
      .lineStyle(0)
      .beginFill(colour, 1)
      .drawPolygon(
        shape.vertices
          .map {
            case Complex(re, im) => new Point(re, im)
          }
          .toJSArray
          .asInstanceOf[scala.scalajs.js.Array[Double | typings.pixiJs.PIXI.Point]]
      )

    application.renderer.generateTexture(graphics, 1, 1)
  }

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
        val s = new Sprite(polygonTexture(entity.colour, entity.shape))
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
      val sprite = dummyMobSprites.getOrElse(entity.id, {
        val s = new Sprite(polygonTexture(0xc0c0c0, entity.shape))
        s.anchor.set(0.5, 0.5)
        dummyMobSprites += (entity.id -> s)
        dummyMobContainer.addChild(s)
        s
      })

      sprite.rotation = -entity.rotation // orientation is reversed...
      camera.viewportManager(sprite, entity.currentPosition(now), entity.shape.boundingBox)
    }

  private val bossesSprites: mutable.Map[Entity.Id, Sprite]        = mutable.Map.empty
  private val bossesRangeIndicator: mutable.Map[Entity.Id, Sprite] = mutable.Map.empty
  private def drawBosses(entities: List[BossEntity], now: Long): Unit =
    entities.foreach { entity =>
      val sprite = bossesSprites.getOrElse(
        entity.id, {
          val s = new Sprite(diskTexture(0xFFFFFF, entity.shape.radius, withBlackDot = true))
          s.anchor.set(0.5, 0.5)
          bossesSprites += (entity.id -> s)
          bossesContainer.addChild(s)
          s
        }
      )

      sprite.rotation = -entity.rotation
      camera.viewportManager(sprite, entity.currentPosition(now), entity.shape.boundingBox)

      val rangeSprite = bossesRangeIndicator.getOrElse(
        entity.id, {
          val s = new Sprite(circleTexture(0xFFFFFF, 0.5, Boss101.meleeRange))
          s.anchor.set(0.5, 0.5)
          bossesRangeIndicator += (entity.id -> s)
          bossesContainer.addChild(s)
          s
        }
      )

      camera.viewportManager(rangeSprite, entity.currentPosition(now), entity.shape.boundingBox)

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
          val s = new Sprite(diskTexture(bullet.colour, bullet.shape.radius))
          s.anchor.set(0.5, 0.5)
          pentagonBullets += (bullet.id -> s)
          movingStuffContainer.addChild(s)
          s
        }
      )
      camera.viewportManager(sprite, bullet.currentPosition(currentTime), bullet.shape.boundingBox)
    }
  }

  private val bulletSprites: mutable.Map[Entity.Id, Sprite] = mutable.Map()
  private def drawSimpleBullets(bullets: List[SimpleBulletBody], currentTime: Long): Unit =
    bullets.foreach { bullet =>
      val sprite = bulletSprites.getOrElse(bullet.id, {
        val s = new Sprite(diskTexture(0x000000, bullet.shape.radius))
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
        val s = new Sprite(polygonTexture(obstacle.colour.intColour, obstacle.shape))
        s.anchor.set(0.5, 0.5)
        obstacleSprites += (obstacle.id -> s)
        obstacleContainer.addChild(s)
        s
      }
    )
    camera.viewportManager(sprite, obstacle.pos, obstacle.shape.boundingBox)
  }

  def drawGameState(gameState: GameState, cameraPosition: Complex, currentTime: Long): Unit = {
    camera.worldCenter = cameraPosition
    drawPlayers(gameState.players.values.toList)
    drawSimpleBullets(gameState.simpleBullets.values.toList, currentTime)
    drawDummyMobs(gameState.dummyMobs.values.toList, currentTime)
    drawBosses(gameState.bosses.valuesIterator.toList, currentTime)
    drawPentagonBullets(gameState.pentagonBullets.valuesIterator.toList, currentTime)
    drawObstacles(gameState.allObstacles.toList)
    startButton.update(gameState, camera)
  }

}
