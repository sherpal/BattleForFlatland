package game.ui.gui.reactivecomponents

import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.{Signal, Val}
import game.ui.reactivepixi.AttributeModifierBuilder._
import game.ui.reactivepixi.ReactivePixiElement._
import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.anon.Align
import typings.pixiJs.mod.TextStyle
import utils.misc.RGBColour

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
    gameStateUpdates
      .map {
        case (gameState, currentTime) =>
          (for {
            entity  <- gameState.withAbilityEntitiesById(entityId)
            lastUse <- entity.relevantUsedAbilities.get(abilityId)
            elapsedTime = currentTime - lastUse.time
            cooldown    = lastUse.cooldown
          } yield (cooldown - elapsedTime) / cooldown.toDouble)
            .getOrElse(0.0)
      }
      .toSignal(0.0),
    Val(colour),
    gameStateUpdates
      .map {
        case (gameState, currentTime) =>
          (
            for {
              entity  <- gameState.withAbilityEntitiesById(entityId)
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
      Align().setFontSize(10).setFill(colour.matchingTextColour.rgb)
    )
  )

  container.amend(bar, nameText)
}
