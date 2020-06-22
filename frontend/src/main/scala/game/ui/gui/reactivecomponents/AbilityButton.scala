package game.ui.gui.reactivecomponents

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventstream.EventStream
import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import typings.pixiJs.PIXI.Texture
import game.ui.reactivepixi.ReactivePixiElement._
import game.ui.reactivepixi.AttributeModifierBuilder._
import game.ui.reactivepixi.EventModifierBuilder._
import gamelogic.gamestate.GameState

final class AbilityButton(
    abilityId: Ability.AbilityId,
    playerId: Entity.Id,
    clickWriter: Observer[Ability.AbilityId],
    texture: Texture,
    overlayTexture: Texture,
    gameStateUpdates: EventStream[(GameState, Long)]
) extends GUIComponent {

  container.amend(
    pixiSprite(
      texture,
      interactive := true,
      onClick.stopPropagation().mapTo(abilityId) --> clickWriter
    ),
    new StatusBar(gameStateUpdates): ReactiveContainer
  )

}
