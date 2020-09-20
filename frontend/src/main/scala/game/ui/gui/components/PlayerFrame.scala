package game.ui.gui.components

import assets.Asset
import com.raquo.airstream.core.Observer
import game.ui.gui.components.buffs.BuffContainer
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.interaction.{InteractionEvent, InteractionEventTypes}
import typings.pixiJs.PIXI.{LoaderResource, Texture}
import typings.pixiJs.anon.Align
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
    resources: PartialFunction[Asset, LoaderResource],
    targetFromGUIWriter: Observer[Entity.Id]
) extends GUIComponent {

  container.visible = false

  private var _width: Double  = startWidth
  private var _height: Double = startHeight

  private val shapeSprite          = new Sprite(entityShapeTexture)
  private val backgroundLifeSprite = new Sprite(lifeTexture)
  private val lifeSprite           = new Sprite(lifeTexture)
  private val lifeMask             = new Graphics()
  lifeSprite.mask = lifeMask
  private val resourceSprite = new Sprite(resourceTexture)
  private val resourceMask   = new Graphics()
  resourceSprite.mask = resourceMask

  val buffContainer = new BuffContainer(entityId, resources)
  buffContainer.container.y = _height

  private val playerNameText = new Text(
    "",
    new TextStyle(
      Align(
        fontSize = 10.0
      )
    )
  )

  private val lifeText = new Text(
    "",
    new TextStyle(Align(fontSize = 15.0))
  )

  List(shapeSprite, backgroundLifeSprite, lifeSprite, lifeMask, resourceSprite, resourceMask, playerNameText, lifeText)
    .foreach(
      container.addChild
    )

  container.interactive = true
  container.addListener(
    InteractionEventTypes.click, { (event: InteractionEvent) =>
      event.stopPropagation()
      scala.scalajs.js.timers.setTimeout(100.0) {
        targetFromGUIWriter.onNext(entityId)
      }
      ()
    }
  )

  private var _isSetup: Boolean = false

  def setUp(gameState: GameState, width: Double, height: Double): Unit = gameState.players.get(entityId).foreach {
    entity =>
      _isSetup = true
      _width   = width
      _height  = height

      container.visible         = true
      lifeSprite.tint           = 0x00FF00
      backgroundLifeSprite.tint = 0xc0c0c0
      resourceSprite.tint       = entity.resourceType.colour.intColour

      shapeSprite.width  = height
      shapeSprite.height = height

      lifeSprite.x               = height
      lifeSprite.width           = width - height
      lifeMask.x                 = height
      backgroundLifeSprite.x     = lifeSprite.x
      backgroundLifeSprite.width = lifeSprite.width

      val lifeProportion = 0.8
      lifeSprite.height           = height * lifeProportion
      backgroundLifeSprite.height = lifeSprite.height

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

      lifeText.x = width - 30

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

      lifeText.text = entity.life.toString
    case None =>
      container.visible = false
  }

}
