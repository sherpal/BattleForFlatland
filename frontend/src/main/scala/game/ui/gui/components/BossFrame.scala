package game.ui.gui.components

import com.raquo.airstream.core.Observer
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.PIXI.interaction.{InteractionEvent, InteractionEventTypes}
import typings.pixiJs.anon.Align
import typings.pixiJs.mod.{Graphics, Sprite, Text, TextStyle}
import utils.misc.RGBColour

final class BossFrame(
    val entityId: Entity.Id,
    backgroundTexture: Texture,
    lifeTexture: Texture,
    castingBarTexture: Texture,
    targetFromGUIWriter: Observer[Entity.Id]
) extends GUIComponent {

  container.visible = true

  container.interactive = true
  container.addListener(InteractionEventTypes.click, { event: InteractionEvent =>
    targetFromGUIWriter.onNext(entityId)
  })

  val width  = 200.0
  val height = 15.0

  private val backgroundSprite = new Sprite(backgroundTexture)
  backgroundSprite.width  = width
  backgroundSprite.height = height
  container.addChild(backgroundSprite)

  private val lifeSprite = new Sprite(lifeTexture)
  lifeSprite.width  = width
  lifeSprite.height = height * 2 / 3
  lifeSprite.tint   = RGBColour.green.intColour
  private val lifeMask = new Graphics
  lifeSprite.mask = lifeMask
  container.addChild(lifeSprite)
  container.addChild(lifeMask)

  private val castingBar = new Sprite(castingBarTexture)
  castingBar.width  = width
  castingBar.y      = lifeSprite.height
  castingBar.height = height * 1 / 3
  castingBar.tint   = RGBColour.red.intColour
  private val castingBarMask = new Graphics
  castingBarMask.y = castingBar.y
  castingBar.mask  = castingBarMask
  container.addChild(castingBar)
  container.addChild(castingBarMask)

  private val bossNameText = new Text(
    "",
    new TextStyle(
      Align(
        fontSize = 10.0
      )
    )
  )
  container.addChild(bossNameText)

  override def update(gameState: GameState, currentTime: Long): Unit = {
    gameState.castingEntityInfo.get(entityId) match {
      case Some(castingInfo) =>
        castingBar.visible = true
        val currentCastTime    = (currentTime - castingInfo.startedTime).toDouble
        val castingProgression = currentCastTime / castingInfo.ability.castingTime
        castingBarMask.clear().beginFill(0xc0c0c0).drawRect(0, 0, width * castingProgression, castingBar.height)
      case None =>
        castingBar.visible = false
    }

    gameState.bosses.get(entityId) match {
      case Some(boss) =>
        bossNameText.text = boss.name

        lifeMask.clear().beginFill(0xc0c0c0).drawRect(0, 0, width * boss.life / boss.maxLife, lifeSprite.height)
      case None =>
        bossNameText.visible = false
    }
  }

}
