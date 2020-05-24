package game.ui.gui.components

import com.raquo.airstream.signal.SignalViewer
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.{LivingEntity, MovingBody}
import gamelogic.gamestate.GameState
import typings.pixiJs.AnonAlign
import typings.pixiJs.PIXI.{RenderTexture, Texture}
import typings.pixiJs.mod.{Text, TextStyle}
import utils.misc.RGBColour

final class TargetFrame($maybeTarget: SignalViewer[Option[MovingBody with LivingEntity]], barTexture: Texture)
    extends GUIComponent {

  val width = 150.0

  private def maybeTarget(gameState: GameState) = $maybeTarget.now.map(_.id).flatMap(gameState.livingEntityById)

  private val bar = new StatusBar(
    (gameState: GameState, _: Long) => {
      maybeTarget(gameState)
        .fold(0.0) { entity =>
          entity.life / entity.maxLife
        }
    },
    (_, _) => RGBColour(0, 255, 0), { (gameState, _) =>
      maybeTarget(gameState).isDefined
    },
    barTexture
  )

  private val text = new Text(
    "",
    new TextStyle(
      AnonAlign(
        fontSize = 10.0
      )
    )
  )

  private val lifeText = new Text(
    "",
    new TextStyle(
      AnonAlign(
        fontSize = 15.0
      )
    )
  )

  container.addChild(bar.container)
  container.addChild(text)
  container.addChild(lifeText)
  bar.setSize(width, 30)
  lifeText.x = width - 40

  def update(gameState: GameState, currentTime: Long): Unit = {
    bar.update(gameState, currentTime)
    maybeTarget(gameState).foreach {
      case entity: BossEntity  => text.text = entity.name
      case entity: PlayerClass => text.text = entity.name
      case entity              => text.text = entity.id.toString
    }
  }

}
