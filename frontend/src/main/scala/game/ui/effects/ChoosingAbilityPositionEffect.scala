package game.ui.effects

import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.Owner
import game.Camera
import game.ui.Drawer
import gamelogic.abilities.Ability
import gamelogic.physics.Complex
import gamelogic.physics.shape.BoundingBox
import typings.pixiJs.mod.{Application, Container, Sprite}
import typings.pixiJs.PIXI.DisplayObject
import gamelogic.entities.Entity

final class ChoosingAbilityPositionEffect(
    val application: Application,
    container: Container,
    camera: Camera,
    choosingAbilityPositions: EventStream[(Complex, Option[Ability.AbilityId])]
)(implicit owner: Owner)
    extends Drawer {

  application.stage.addChild(container)

  private var maybeCurrentSprite: Option[Sprite] = Option.empty

  def createSmallDot(): Sprite = {
    val sprite = new Sprite(diskTexture(0xFF0000, 1.0, 5))
    sprite.anchor.set(0.5)
    sprite
  }

  choosingAbilityPositions.foreach {
    case (_, None) =>
      application.view.style.cursor = "default"
      maybeCurrentSprite.foreach { sprite =>
        maybeCurrentSprite = None
        sprite.destroy()
      }
    case (mousePosition, Some(_)) =>
      maybeCurrentSprite = maybeCurrentSprite.orElse(Some(createSmallDot()))
      maybeCurrentSprite.foreach { sprite =>
        camera.viewportManager(sprite, mousePosition, BoundingBox(-1, -1, 1, 1))
        if (!container.children.contains(sprite))
          container.addChild(sprite)
      }
      application.view.style.cursor = "pointer"

  }

  def maybeEntityDisplayObjectById(entityId: Entity.Id): Option[DisplayObject] = None

}
