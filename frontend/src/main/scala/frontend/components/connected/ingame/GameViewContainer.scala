package frontend.components.connected.ingame

import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.{Component, LifecycleComponent}
import org.scalajs.dom.html
import com.raquo.laminar.api.L._
import gamelogic.gamestate.GameState
import typings.pixiJs.{AnonAntialias => ApplicationOptions}
import typings.pixiJs.mod.{Application, Sprite, Text, Texture}
import utils.pixi.monkeypatching.PIXIPatching._

/**
  * The GameViewContainer is responsible for drawing the game, and only that.
  *
  * It received an initial [[gamelogic.gamestate.GameState]] and a streams of game states, and it has to draw everything.
  * The way it does that is its business, but it will probably be customizable in the future.
  *
  * It will most likely be drawing on a canvas using Pixi, and perhaps using svg for life bars and stuff.
  */
final class GameViewContainer private (initialGameState: GameState, $gameState: EventStream[GameState])
    extends LifecycleComponent[html.Div] {

  private def container = elem.ref

  val currentGameState: Var[GameState] = Var(initialGameState)

  val elem: ReactiveHtmlElement[html.Div] = div(
    className := "GameViewContainer"
  )

  override def componentDidMount(): Unit = {

    $gameState.foreach(currentGameState.set)(elem)

    val application = new Application(ApplicationOptions(backgroundColor = 0x1099bb))
    container.appendChild(application.view.asInstanceOf[html.Canvas])

    val text = new Text("Some text")
    text.x = 100
    text.y = 20

    application.stage.addChild(text)

    val tickerFn = (_: Double) => {
      val gameState = currentGameState.now

      text.text = gameState.time.toString
    }

    application.ticker.add(tickerFn)

  }

}

object GameViewContainer {
  def apply(initialGameState: GameState, $gameState: EventStream[GameState]): GameViewContainer =
    new GameViewContainer(initialGameState, $gameState)
}
