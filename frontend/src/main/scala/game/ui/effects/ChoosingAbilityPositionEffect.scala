package game.ui.effects

import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.Owner
import game.{Camera, Mouse}
import game.ui.Drawer
import gamelogic.abilities.Ability
import gamelogic.physics.Complex
import gamelogic.physics.shape.BoundingBox
import typings.pixiJs.mod.Sprite
import typings.pixiJs.mod.{Application, Container}

final class ChoosingAbilityPositionEffect(
    val application: Application,
    container: Container,
    camera: Camera,
    choosingAbilityPositions: EventStream[(Complex, Option[Ability.AbilityId])]
)(implicit owner: Owner)
    extends Drawer {

  application.stage.addChild(container)

  private var maybeCurrentSprite: Option[Sprite] = Option.empty

  def createSmallDot(): Sprite = new Sprite(diskTexture(0xFF0000, 1.0, 5))

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
      }
      application.view.style.cursor = "pointer"

  }

}
