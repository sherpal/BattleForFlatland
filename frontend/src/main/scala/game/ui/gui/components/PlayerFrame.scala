package game.ui.gui.components

import assets.Asset
import game.ui.gui.components.buffs.BuffContainer
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import typings.pixiJs.AnonAlign
import typings.pixiJs.PIXI.{LoaderResource, Texture}
import typings.pixiJs.mod.{Graphics, Sprite, Text, TextStyle}

/**
  * Creates a container whose content is attached to the player with given id.
  *
  * In order to kick off things, you should call the setUp method (but only once the entity exists), and then on each
  * update you should call the update method.
  */
final class PlayerFrame(
    val entityId: Entity.Id,
    entityShapeTexture: Texture,
    lifeTexture: Texture,
    resourceTexture: Texture,
    startWidth: Double,
    startHeight: Double,
    resources: PartialFunction[Asset, LoaderResource]
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

  val buffContainer = new BuffContainer(entityId, resources)
  buffContainer.container.y = _height

  private val playerNameText = new Text(
    "",
    new TextStyle(
      AnonAlign(
        fontSize = 10.0
      )
    )
  )

  List(shapeSprite, lifeSprite, lifeMask, resourceSprite, resourceMask, playerNameText).foreach(container.addChild)

  private var _isSetup: Boolean = false

  def setUp(gameState: GameState, width: Double, height: Double): Unit = gameState.players.get(entityId).foreach {
    entity =>
      _isSetup = true
      _width   = width
      _height  = height

      container.visible   = true
      lifeSprite.tint     = 0x00FF00
      resourceSprite.tint = entity.resourceType.colour.intColour

      shapeSprite.width  = height
      shapeSprite.height = height

      lifeSprite.x     = height
      lifeSprite.width = width - height
      lifeMask.x       = height

      val lifeProportion = 0.8
      lifeSprite.height = height * lifeProportion

      playerNameText.x    = height + 4
      playerNameText.y    = 2
      playerNameText.tint = 0xFFFFFF
      playerNameText.text = entity.name

      resourceSprite.x      = height
      resourceSprite.y      = height * lifeProportion
      resourceSprite.width  = width - height
      resourceSprite.height = height * (1 - lifeProportion)
      resourceMask.x        = height
      resourceMask.y        = height * lifeProportion

      container.addChild(buffContainer.container)
      buffContainer.container.y = height

  }

  private def adaptMask(mask: Graphics, parentSprite: Sprite, ratio: Double): Unit =
    mask.clear().beginFill(0x000000).drawRect(0, 0, parentSprite.width * ratio, parentSprite.height)

  def update(gameState: GameState, currentTime: Long): Unit = gameState.players.get(entityId) match {
    case Some(entity) =>
      if (!_isSetup) setUp(gameState, _width, _height)
      container.visible = true
      val lifeRatio     = entity.life / entity.maxLife
      val resourceRatio = entity.resourceAmount.amount / entity.maxResourceAmount

      adaptMask(lifeMask, lifeSprite, lifeRatio)
      adaptMask(resourceMask, resourceSprite, resourceRatio)

      buffContainer.update(gameState, currentTime)
    case None =>
      container.visible = false
  }

}
