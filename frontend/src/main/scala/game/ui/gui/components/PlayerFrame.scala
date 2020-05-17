package game.ui.gui.components

import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.mod.{Graphics, Sprite}

/**
  * Creates a container whose content is attached to the player with given id.
  *
  * In order to kick off things, you should call the setUp method (but only once the entity exists), and then on each
  * update you should call the update method.
  */
final class PlayerFrame(
    entityId: Entity.Id,
    entityShapeTexture: Texture,
    lifeTexture: Texture,
    resourceTexture: Texture,
    startWidth: Double,
    startHeight: Double
) extends GUIComponent {

  container.visible = false

  private var _width: Double  = startWidth
  private var _height: Double = startHeight

  private val shapeSprite = new Sprite(entityShapeTexture)
  private val lifeSprite  = new Sprite(lifeTexture)
  private val lifeMask    = new Graphics()
  lifeSprite.mask = lifeMask
  private val resourceSprite = new Sprite(resourceTexture)
  private val resourceMask   = new Graphics()
  resourceSprite.mask = resourceMask

  List(shapeSprite, lifeSprite, lifeMask, resourceSprite, resourceMask).foreach(container.addChild)

  private var _isSetup: Boolean = false

  def setUp(gameState: GameState, width: Double, height: Double): Unit = gameState.players.get(entityId).foreach {
    entity =>
      _isSetup = true
      _width   = width
      _height  = height

      container.visible   = true
      lifeSprite.tint     = 0x00FF00
      resourceSprite.tint = entity.resourceType.colour.intColour

      //container.width  = width
      //container.height = height

      shapeSprite.width  = height
      shapeSprite.height = height

      lifeSprite.x     = height
      lifeSprite.width = width - height
      lifeMask.x       = height

      lifeSprite.height = height * 0.7

      resourceSprite.x      = height
      resourceSprite.y      = height * 0.7
      resourceSprite.width  = width - height
      resourceSprite.height = height * 0.3
      resourceMask.x        = height
      resourceMask.y        = height * 0.7

  }

  private def adaptMask(mask: Graphics, parentSprite: Sprite, ratio: Double): Unit =
    mask.clear().beginFill(0x000000).drawRect(0, 0, parentSprite.width * ratio, parentSprite.height)

  def update(gameState: GameState): Unit = gameState.players.get(entityId) match {
    case Some(entity) =>
      if (!_isSetup) setUp(gameState, _width, _height)
      container.visible = true
      val lifeRatio     = entity.life / entity.maxLife
      val resourceRatio = entity.resourceAmount.amount / entity.maxResourceAmount

      adaptMask(lifeMask, lifeSprite, lifeRatio)
      adaptMask(resourceMask, resourceSprite, resourceRatio)
    case None =>
      container.visible = false
  }

}
