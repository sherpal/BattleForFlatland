package game

import gamelogic.entities.{DummyLivingEntity, Entity, SimpleBulletBody}
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import org.scalajs.dom.html
import typings.pixiJs.PIXI
import typings.pixiJs.mod._

import scala.collection.mutable

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

  private val players: mutable.Map[Entity.Id, Sprite] = mutable.Map()

  private def circleTexture(colour: Int, radius: Double): PIXI.RenderTexture = {
    val graphics = new Graphics
    graphics.lineStyle(0) // draw a circle, set the lineStyle to zero so the circle doesn't have an outline

    graphics.beginFill(colour, 1)
    graphics.drawCircle(0, 0, radius)

    application.renderer.generateTexture(graphics, 1, 1)
  }

  private def drawPlayers(entities: List[DummyLivingEntity]): Unit =
    entities.foreach { entity =>
      val sprite = players.getOrElse(entity.id, {
        val s = new Sprite(circleTexture(entity.colour, entity.shape.radius))
        s.anchor.set(0.5, 0.5)
        players += (entity.id -> s)
        playerContainer.addChild(s)
        s
      })

      camera.viewportManager(sprite, entity.pos, entity.shape.boundingBox)
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
  }

}
