package game.ui.gui.reactivecomponents

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.{Signal, Val}
import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import typings.pixiJs.PIXI.Texture
import game.ui.reactivepixi.ReactivePixiElement._
import game.ui.reactivepixi.AttributeModifierBuilder._
import game.ui.reactivepixi.EventModifierBuilder._
import gamelogic.gamestate.GameState
import utils.misc.RGBAColour

final class AbilityButton(
    abilityId: Ability.AbilityId,
    playerId: Entity.Id,
    clickWriter: Observer[Ability.AbilityId],
    texture: Texture,
    overlayTexture: Texture,
    gameStateUpdates: EventStream[(GameState, Long)],
    dimensions: Signal[(Double, Double)]
) extends GUIComponent {

  container.amend(
    pixiSprite(
      texture,
      interactive := true,
      onClick.stopPropagation.mapTo(abilityId) --> clickWriter,
      width <-- dimensions.map(_._1),
      height <-- dimensions.map(_._2)
    ),
    new StatusBar(
      gameStateUpdates.map {
        case (gameState, currentTime) =>
          (for {
            player <- gameState.players.get(playerId)
            lastUse <- player.relevantUsedAbilities.get(abilityId)
            elapsedTime = currentTime - lastUse.time
            value       = 1.0 - elapsedTime / lastUse.cooldown.toDouble
          } yield value max 0.0).getOrElse(0.0)

      },
      Val(RGBAColour(0, 0, 0, 0.4)),
      Val(true),
      overlayTexture,
      dimensions,
      orientation = StatusBar.Vertical
    ): ReactiveContainer
  )

}
