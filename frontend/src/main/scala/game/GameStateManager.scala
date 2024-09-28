package game

import indigo.*
import gamelogic.gamestate.GameState
import indigo.scenes.SceneName
import indigo.scenes.Scene
import com.raquo.laminar.api.L.*
import gamelogic.gamestate.AddAndRemoveActions
import models.bff.ingame.InGameWSProtocol
import gamelogic.entities.Entity
import gamelogic.physics.Complex
import com.raquo.airstream.ownership.Owner
import game.scenes.loading.LoadingScene
import game.scenes.ingame.InGameScene
import models.bff.ingame.Controls
import assets.fonts.Fonts

class GameStateManager(
    userName: String,
    initialGameState: GameState,
    actionsFromServerEvents: EventStream[AddAndRemoveActions],
    allPlayersAreReadyEvents: EventStream[Unit],
    socketOutWriter: Observer[InGameWSProtocol.Outgoing],
    playerId: Entity.Id,
    bossStartingPosition: Complex,
    deltaTimeWithServer: Seconds,
    controls: Controls,
    fonts: Fonts
)(using Owner)
    extends IndigoGame[
      InGameScene.StartupData,
      InGameScene.StartupData,
      IndigoModel,
      IndigoViewModel
    ] {

  val backendCommWrapper =
    BackendCommWrapper(allPlayersAreReadyEvents, actionsFromServerEvents, socketOutWriter.onNext(_))

  override def initialModel(startupData: InGameScene.StartupData): Outcome[IndigoModel] =
    Outcome(IndigoModel.initial(initialGameState))

  override def present(
      context: FrameContext[InGameScene.StartupData],
      model: IndigoModel,
      viewModel: IndigoViewModel
  ): Outcome[SceneUpdateFragment] = Outcome(SceneUpdateFragment.empty)

  override def scenes(
      bootData: InGameScene.StartupData
  ): NonEmptyList[Scene[InGameScene.StartupData, IndigoModel, IndigoViewModel]] =
    NonEmptyList[Scene[InGameScene.StartupData, IndigoModel, IndigoViewModel]](
      LoadingScene(userName, "in-game/gui", backendCommWrapper),
      InGameScene(
        playerId,
        gameAction =>
          socketOutWriter.onNext(InGameWSProtocol.GameActionWrapper(Vector(gameAction))),
        backendCommWrapper,
        deltaTimeWithServer,
        controls
      )
    )

  override def updateModel(
      context: FrameContext[InGameScene.StartupData],
      model: IndigoModel
  ): GlobalEvent => Outcome[IndigoModel] = _ => Outcome(model)

  override def updateViewModel(
      context: FrameContext[InGameScene.StartupData],
      model: IndigoModel,
      viewModel: IndigoViewModel
  ): GlobalEvent => Outcome[IndigoViewModel] = _ => Outcome(viewModel)

  override def boot(
      flags: Map[String, String]
  ): Outcome[BootResult[InGameScene.StartupData, IndigoModel]] = {
    val width  = flags("width").toInt
    val height = flags("height").toInt
    Outcome(
      BootResult(GameConfig(width, height), InGameScene.StartupData(Rectangle(width, height)))
        .withFonts(fonts.fontsInfo.values.toSet)
    )
  }
  override def initialViewModel(
      startupData: InGameScene.StartupData,
      model: IndigoModel
  ): Outcome[IndigoViewModel] =
    Outcome(
      IndigoViewModel.initial(
        model.inGameState.actionGatherer.currentGameState
          .applyActions(model.inGameState.unconfirmedActions),
        bossStartingPosition,
        startupData,
        playerId
      )
    )

  override def setup(
      bootData: InGameScene.StartupData,
      assetCollection: AssetCollection,
      dice: Dice
  ): Outcome[Startup[InGameScene.StartupData]] = Outcome(Startup.Success(bootData))

  override def eventFilters: EventFilters =
    EventFilters.BlockAll

  override def initialScene(bootData: InGameScene.StartupData): Option[SceneName] = Option.empty

}
