package game.ui.gui.components

import com.raquo.airstream.core.Observer
import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.PIXI.interaction.{InteractionEvent, InteractionEventTypes}
import typings.pixiJs.mod.Sprite
import utils.misc.RGBAColour

final class AbilityButton(
    val abilityId: Ability.AbilityId,
    playerId: Entity.Id,
    clickWriter: Observer[Ability.AbilityId],
    texture: Texture,
    overlayTexture: Texture
) extends GUIComponent {

  private val sprite = new Sprite(texture)
  container.addChild(sprite)

  sprite.interactive = true
  sprite.addListener(InteractionEventTypes.click, (event: InteractionEvent) => {
    event.stopPropagation()
    clickWriter.onNext(abilityId)
  })

  private val cdOverlay = new StatusBar(
    { (gameState, currentTime) =>
      (for {
        player  <- gameState.players.get(playerId)
        lastUse <- player.relevantUsedAbilities.get(abilityId)
        elapsedTime = currentTime - lastUse.time
        value       = 1.0 - elapsedTime / lastUse.cooldown.toDouble
      } yield value max 0.0).getOrElse(0.0)

    },
    (_, _) => RGBAColour(0, 0, 0, 0.4),
    (_, _) => true,
    overlayTexture,
    orientation = StatusBar.Vertical
  )
  container.addChild(cdOverlay.container)

  /** square image */
  def setSize(size: Double): Unit = {
    sprite.width  = size
    sprite.height = size
    cdOverlay.setSize(size, size)
  }

  def update(gameState: GameState, currentTime: Long): Unit =
    cdOverlay.update(gameState, currentTime)

}
