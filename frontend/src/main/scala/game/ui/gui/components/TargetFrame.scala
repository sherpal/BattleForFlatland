package game.ui.gui.components

import com.raquo.airstream.signal.SignalViewer
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.{LivingEntity, MovingBody}
import gamelogic.gamestate.GameState
import typings.pixiJs.AnonAlign
import typings.pixiJs.PIXI.{RenderTexture, Texture}
import typings.pixiJs.mod.{Text, TextStyle}

final class TargetFrame($maybeTarget: SignalViewer[Option[MovingBody with LivingEntity]], barTexture: Texture)
    extends GUIComponent {

  private def maybeTarget(gameState: GameState) = $maybeTarget.now.map(_.id).flatMap(gameState.livingEntityById)

  private val bar = new StatusBar(
    (gameState: GameState, _: Long) => {
      maybeTarget(gameState)
        .fold(0.0) { entity =>
          entity.life / entity.maxLife
        }
    },
    (_, _) => 0x00FF00, { (gameState, _) =>
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

  container.addChild(bar.container)
  container.addChild(text)
  bar.setSize(150, 30)

  def update(gameState: GameState, currentTime: Long): Unit = {
    bar.update(gameState, currentTime)
    maybeTarget(gameState).foreach {
      case entity: BossEntity  => text.text = entity.name
      case entity: PlayerClass => text.text = entity.name
      case entity              => text.text = entity.id.toString
    }
  }

}
