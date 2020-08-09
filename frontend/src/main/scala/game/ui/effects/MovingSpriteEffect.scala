package game.ui.effects

import game.Camera
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import gamelogic.physics.shape.BoundingBox
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.mod.{Container, Sprite}

/**
  * Creates an effect which follows the given `path`. A sprite with the given `texture` is created. The position in
  * the world of this effect will be the given by the path current position, translated by the `worldAnchorPosition`
  * function.
  *
  * @param texture texture for the effect sprite
  * @param startTime starting time of the effect
  * @param path path that the effect must follow
  * @param rotation gives the rotation of the sprite as function of current [[gamelogic.gamestate.GameState]] and time
  * @param worldAnchorPosition translation of the effect given the current [[gamelogic.gamestate.GameState]] and time
  * @param anchor anchor to set the sprite of
  */
final class MovingSpriteEffect(
    texture: Texture,
    startTime: Long,
    path: Path,
    rotation: (GameState, Long) => Double,
    worldAnchorPosition: (GameState, Long) => Complex,
    camera: Camera,
    anchor: (Double, Double) = (0.5, 0.5),
    boundingBox: BoundingBox = BoundingBox(-1, -1, 1, 1)
) extends GameEffect {

  val sprite = new Sprite(texture)
  sprite.anchor.set(anchor._1, anchor._2)

  def destroy(): Unit = sprite.destroy()

  def update(currentTime: Long, gameState: GameState): Unit = {
    sprite.rotation = -rotation(gameState, currentTime - startTime)
    val position = path(currentTime - startTime) + worldAnchorPosition(gameState, currentTime)

    camera.viewportManager(
      sprite,
      position,
      boundingBox
    )
  }

  def isOver(currentTime: Long, gameState: GameState): Boolean = path.isOver(currentTime - startTime)

  def addToContainer(container: Container): Unit = container.addChild(sprite)
}
