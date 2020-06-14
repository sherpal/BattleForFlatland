package game.ui.effects

import game.Camera
import gamelogic.physics.Complex
import gamelogic.physics.shape.BoundingBox
import typings.pixiJs.anon.Align
import typings.pixiJs.mod.{Container, Text, TextStyle}
import utils.misc.RGBAColour

import scala.util.Random

/**
  * Creates a simple effect where the given text follows the given movement in time.
  *
  *
  * @param text to be displayed
  * @param colour to give to the text
  * @param startTime time at which to start the animation
  * @param path [[game.ui.effects.Path]] to describe the position of the text in function of time. The position has to
  *                 be in game world coordinate space.
  */
final class SimpleTextEffect(
    text: String,
    colour: RGBAColour,
    startTime: Long,
    path: Path,
    camera: Camera,
    fontSize: Double = 20
) extends GameEffect {

  val pixiText = new Text(
    text,
    new TextStyle(
      Align(
        fontSize = fontSize,
        fill     = colour.rgb
      )
    )
  )
  pixiText.anchor.set(0.5, 0.5)

  override def addToContainer(container: Container): Unit = container.addChild(pixiText)

  val boundingBox: BoundingBox = BoundingBox(-pixiText.width, -pixiText.height, pixiText.width, pixiText.height)

  def isOver(currentTime: Long): Boolean    = path.isOver(currentTime - startTime)
  def isStarted(currentTime: Long): Boolean = currentTime > startTime

  def destroy(): Unit = pixiText.destroy()

  def update(currentTime: Long): Unit =
    if (isOver(currentTime) || !isStarted(currentTime)) pixiText.visible = false
    else {
      pixiText.visible = true
      camera.viewportManager(pixiText, path(currentTime - startTime), boundingBox)
    }

}
