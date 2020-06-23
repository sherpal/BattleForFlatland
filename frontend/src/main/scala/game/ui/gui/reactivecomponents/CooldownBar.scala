package game.ui.gui.reactivecomponents

import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.{Signal, Val}
import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.Texture
import utils.misc.RGBColour
import game.ui.reactivepixi.ReactivePixiElement._
import game.ui.reactivepixi.AttributeModifierBuilder._
import typings.pixiJs.anon.Align
import typings.pixiJs.mod.TextStyle

final class CooldownBar(
    entityId: Entity.Id,
    val abilityId: Ability.AbilityId,
    abilityName: String,
    colour: RGBColour,
    texture: Texture,
    gameStateUpdates: EventStream[(GameState, Long)],
    dimensions: Signal[(Double, Double)]
) extends GUIComponent {

  val bar: StatusBar = new StatusBar(
    gameStateUpdates.map {
      case (gameState, currentTime) =>
        (for {
          entity <- gameState.withAbilityEntitiesById(entityId)
          lastUse <- entity.relevantUsedAbilities.get(abilityId)
          elapsedTime = currentTime - lastUse.time
          cooldown    = lastUse.cooldown
        } yield (cooldown - elapsedTime) / cooldown.toDouble)
          .getOrElse(0.0)
    },
    Val(colour),
    gameStateUpdates
      .map {
        case (gameState, currentTime) =>
          (
            for {
              entity <- gameState.withAbilityEntitiesById(entityId)
              lastUse <- entity.relevantUsedAbilities.get(abilityId)
            } yield lastUse.cooldown > currentTime - lastUse.time
          ).getOrElse(false)
      }
      .startWith(false),
    texture,
    dimensions
  )

  val nameText: ReactiveText = pixiText(
    abilityName,
    textStyle := new TextStyle(
      Align(
        fontSize = 10.0
      )
    )
  )

  container.amend(bar, nameText)
}
