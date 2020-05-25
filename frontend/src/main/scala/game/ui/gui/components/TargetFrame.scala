package game.ui.gui.components

import com.raquo.airstream.signal.SignalViewer
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.{LivingEntity, MovingBody}
import gamelogic.gamestate.GameState
import typings.pixiJs.AnonAlign
import typings.pixiJs.PIXI.Texture
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

  private val castingBar = new StatusBar(
    { (gameState, currentTime) =>
      maybeTarget(gameState).flatMap(entity => gameState.castingEntityInfo.get(entity.id)).fold(0.0) { info =>
        (currentTime - info.startedTime) / info.castingTime.toDouble
      }
    }, { (_, _) =>
      RGBColour.red
    }, { (gameState, _) =>
      maybeTarget(gameState).fold(false)(entity => gameState.entityIsCasting(entity.id))
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
  container.addChild(castingBar.container)
  container.addChild(text)
  container.addChild(lifeText)
  bar.setSize(width, 30)
  castingBar.setSize(width, 5)
  castingBar.container.y = 30
  lifeText.x             = width - 40

  def update(gameState: GameState, currentTime: Long): Unit = {
    bar.update(gameState, currentTime)
    castingBar.update(gameState, currentTime)
    maybeTarget(gameState).foreach {
      case entity: BossEntity =>
        text.text     = entity.name
        lifeText.text = entity.life.toString
      case entity: PlayerClass =>
        text.text     = entity.name
        lifeText.text = entity.life.toString
      case entity => text.text = entity.id.toString
    }
  }

}
