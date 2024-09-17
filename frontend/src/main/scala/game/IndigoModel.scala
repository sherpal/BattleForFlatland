package game

import scala.scalajs.js
import gamelogic.gamestate.ActionGatherer
import game.scenes.loading.LoadingState
import gamelogic.gamestate.GameState
import game.scenes.ingame.InGameScene
import indigo.Seconds

class IndigoModel(
    val inGameState: InGameScene.InGameModel,
    val loadingState: LoadingState
) extends js.Object {

  inline def withLoadingState(loadingStateUpdate: LoadingState): IndigoModel =
    IndigoModel(inGameState, loadingStateUpdate)

  inline def modifyLoadingState(fn: LoadingState => LoadingState): IndigoModel =
    withLoadingState(fn(loadingState))

  inline def withInGameState(newInGameState: InGameScene.InGameModel): IndigoModel =
    IndigoModel(newInGameState, loadingState)

}

object IndigoModel {
  def initial(initialGameState: GameState): IndigoModel =
    IndigoModel(
      InGameScene.initialModel(initialGameState),
      LoadingState.NotStarted()
    )
}
