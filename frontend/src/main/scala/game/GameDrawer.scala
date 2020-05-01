package game

import gamelogic.entities.{DummyLivingEntity, Entity}
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import org.scalajs.dom.html
import typings.pixiJs.PIXI
import typings.pixiJs.mod._

import scala.collection.mutable

/**
  * This class can
  */
final class GameDrawer(application: Application) {

  @inline private def stage = application.stage

  val camera: Camera = new Camera(application.view.asInstanceOf[html.Canvas])

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
        players += (entity.id -> s)
        playerContainer.addChild(s)
        s
      })

      camera.viewportManager(sprite, entity.pos, entity.shape.boundingBox)
    }

  def drawGameState(gameState: GameState, cameraPosition: Complex): Unit = {
    camera.worldCenter = cameraPosition
    drawPlayers(gameState.players.values.toList)
  }

}
