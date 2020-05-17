package game.ui

import game.Camera
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.{DummyMob, Entity, SimpleBulletBody}
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import org.scalajs.dom.html
import typings.pixiJs.PIXI
import typings.pixiJs.mod._

import scala.collection.mutable
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|

/**
  * This class is used to draw the game state at any moment in time.
  * The implementation is full of side effects, probably in the wrong fear of less good performance.
  */
final class GameDrawer(application: Application) {

  @inline private def stage = application.stage

  val camera: Camera = new Camera(application.view.asInstanceOf[html.Canvas])

  val bulletContainer: Container = new Container
  stage.addChild(bulletContainer)
  val playerContainer: Container = new Container
  stage.addChild(playerContainer)
  val dummyMobContainer: Container = new Container
  stage.addChild(dummyMobContainer)

  private def circleTexture(colour: Int, radius: Double): PIXI.RenderTexture = {
    val graphics = new Graphics
    graphics.lineStyle(0) // draw a circle, set the lineStyle to zero so the circle doesn't have an outline

    graphics.beginFill(colour, 1)
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

  private val players: mutable.Map[Entity.Id, Sprite] = mutable.Map()

  private def drawPlayers(entities: List[PlayerClass]): Unit =
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

//  private def drawDummyLivingEntity(entities: List[DummyLivingEntity]): Unit =
//    entities.foreach { entity =>
//      val sprite = players.getOrElse(entity.id, {
//        val s = new Sprite(circleTexture(entity.colour, entity.shape.radius))
//        s.anchor.set(0.5, 0.5)
//        players += (entity.id -> s)
//        playerContainer.addChild(s)
//        s
//      })
//
//      camera.viewportManager(sprite, entity.pos, entity.shape.boundingBox)
//    }

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

  private val bulletSprites: mutable.Map[Entity.Id, Sprite] = mutable.Map()
  private def drawSimpleBullets(bullets: List[SimpleBulletBody], currentTime: Long): Unit =
    bullets.foreach { bullet =>
      val sprite = bulletSprites.getOrElse(bullet.id, {
        val s = new Sprite(circleTexture(0x000000, bullet.shape.radius))
        s.anchor.set(0.5, 0.5)
        bulletSprites += (bullet.id -> s)
        bulletContainer.addChild(s)
        s
      })
      camera.viewportManager(sprite, bullet.currentPosition(currentTime), bullet.shape.boundingBox)
    }

  def drawGameState(gameState: GameState, cameraPosition: Complex, currentTime: Long): Unit = {
    camera.worldCenter = cameraPosition
    drawPlayers(gameState.players.values.toList)
    drawSimpleBullets(gameState.simpleBullets.values.toList, currentTime)
    drawDummyMobs(gameState.dummyMobs.values.toList, currentTime)
  }

}
