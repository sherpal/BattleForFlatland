package game.ui.gui.components

import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.anon.Align
import typings.pixiJs.mod.{Text, TextStyle}
import utils.misc.RGBColour

final class CooldownBar(
    entityId: Entity.Id,
    val abilityId: Ability.AbilityId,
    abilityName: String,
    colour: RGBColour,
    texture: Texture
) extends GUIComponent {

  private val bar = new StatusBar(
    { (gameState, currentTime) =>
      (for {
        entity  <- gameState.withAbilityEntitiesById(entityId)
        lastUse <- entity.relevantUsedAbilities.get(abilityId)
        elapsedTime = currentTime - lastUse.time
        cooldown    = lastUse.cooldown
      } yield (cooldown - elapsedTime) / cooldown.toDouble)
        .getOrElse(0.0)
    },
    (_, _) => colour, { (gameState, currentTime) =>
      (
        for {
          entity  <- gameState.withAbilityEntitiesById(entityId)
          lastUse <- entity.relevantUsedAbilities.get(abilityId)
        } yield lastUse.cooldown > currentTime - lastUse.time
      ).getOrElse(false)
    },
    texture
  )

  private val text = new Text(
    abilityName,
    new TextStyle(
      Align().setFontSize(10)
    )
  )

  container.addChild(bar.container)
  container.addChild(text)

  def setSize(width: Double, height: Double): Unit = bar.setSize(width, height)

  def update(gameState: GameState, currentTime: Long): Unit =
    bar.update(gameState, currentTime)

}
