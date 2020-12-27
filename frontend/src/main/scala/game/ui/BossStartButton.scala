package game.ui

import assets.Asset
import com.raquo.airstream.core.Observer
import game.Camera
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import gamelogic.physics.shape.BoundingBox
import typings.pixiJs.PIXI.LoaderResource
import typings.pixiJs.PIXI.{InteractionEvent, InteractionEventTypes}
import typings.pixiJs.anon.Align
import typings.pixiJs.mod.{Text, TextStyle}
import utils.misc.RGBColour

final class BossStartButton(
    val position: Complex,
    resources: PartialFunction[Asset, LoaderResource],
    clickObserver: Observer[Unit]
) {

  val element = new Text(
    "Start fight",
    new TextStyle(
      Align().setFontSize(20).setFill(RGBColour.white.rgb)
    )
  )
  element.anchor.set(0.5, 0.5)
  element.interactive = true
  element.on(InteractionEventTypes.click, { _: InteractionEvent =>
    clickObserver.onNext(())
  })
  val boundingBox: BoundingBox = BoundingBox(-element.width, -element.height, element.width, element.height)

  def update(gameState: GameState, camera: Camera): Unit =
    if (gameState.started) element.visible = false
    else {
      camera.viewportManager(element, position, boundingBox)
    }

}
