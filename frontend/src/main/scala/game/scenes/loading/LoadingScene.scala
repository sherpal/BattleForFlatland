package game.scenes.loading

import indigo.*
import indigo.scenes.*
import indigoextras.subsystems.*
import game.IndigoModel
import game.scenes.loading.LoadingScene.StartupData
import game.IndigoViewModel
import indigo.scenes.SceneEvent.Next
import game.events.CustomIndigoEvents
import game.BackendCommWrapper
import assets.Asset
import models.bff.ingame.InGameWSProtocol

import scala.scalajs.js
import game.scenes.loading.LoadingState.NotStarted
import game.scenes.loading.LoadingState.InProgress
import game.scenes.loading.LoadingState.WaitingForOthers
import indigo.scenes.SceneEvent.JumpTo
import game.scenes.ingame.InGameScene

/** This is the indigo scene when we start the game.
  *
  * It will
  *
  *   - load all assets (using the `AssetBundleLoader` capability)
  *   - when all assets are loaded, it will send a readiness message to the server
  *   - when everyone is ready, it will move on to the in-game scene.
  *
  * Inspiration:
  * https://github.com/PurpleKingdomGames/pirate-demo/blob/main/src/main/scala/pirate/scenes/loading/LoadingScene.scala
  */
class LoadingScene(
    userName: String,
    assetPath: String,
    backendCommWrapper: BackendCommWrapper
) extends Scene[InGameScene.StartupData, IndigoModel, IndigoViewModel] {

  type SceneModel     = LoadingState
  type SceneViewModel = Unit

  override def subSystems: Set[SubSystem[IndigoModel]] =
    Set(AssetBundleLoader[IndigoModel])

  override def present(
      context: SceneContext[InGameScene.StartupData],
      model: SceneModel,
      viewModel: SceneViewModel
  ): Outcome[SceneUpdateFragment] = Outcome(
    SceneUpdateFragment(
      TextBox(
        model match
          case NotStarted()           => "Waiting to start..."
          case InProgress(percentage) => s"Loading... $percentage %"
          case WaitingForOthers() => "All assets loaded, waiting for others to load all assets..."
          case game.scenes.loading.LoadingState.Error(key, message) =>
            s"Outch, something went wrong: $key $message"
        ,
        400,
        30
      )
        .withFontFamily(FontFamily.cursive)
        .withColor(RGBA.White)
        .withFontSize(Pixels(16))
        .withStroke(TextStroke(RGBA.Red, Pixels(1)))
        .withPosition(Point(10, 100))
    )
  )

  override def updateModel(
      context: SceneContext[InGameScene.StartupData],
      loadingState: SceneModel
  ): GlobalEvent => Outcome[SceneModel] = {
    case FrameTick =>
      loadingState match
        case LoadingState.NotStarted() =>
          Outcome(LoadingState.InProgress(0))
            .addGlobalEvents(
              AssetBundleLoaderEvent.Load(
                BindingKey("Loading"),
                Asset.allAssets.map(_.asIndigoAssetType)
              )
            )
        case _ if backendCommWrapper.allPlayersAreReady =>
          Outcome(loadingState).addGlobalEvents(CustomIndigoEvents.BackendCommEvent.EveryoneIsReady)
        case _ => Outcome(loadingState)

    case assetLoadingEvent: AssetBundleLoaderEvent =>
      assetLoadingEvent match
        case indigoextras.subsystems.AssetBundleLoaderEvent.Load(key, assets) =>
          Outcome(loadingState)
        case AssetBundleLoaderEvent.Retry(key) =>
          Outcome(loadingState)
        case AssetBundleLoaderEvent.Started(key) =>
          Outcome(loadingState)
        case AssetBundleLoaderEvent.LoadProgress(key, percent, completed, total) =>
          Outcome(LoadingState.InProgress(percent))
        case AssetBundleLoaderEvent.Success(key) =>
          backendCommWrapper.sendMessageToBackend(InGameWSProtocol.ReadyToStart(userName))
          Outcome(LoadingState.WaitingForOthers())
        case AssetBundleLoaderEvent.Failure(key, message) =>
          Outcome(LoadingState.Error(key.toString, message))

    case CustomIndigoEvents.BackendCommEvent.EveryoneIsReady =>
      println("Moving scene")
      Outcome(loadingState).addGlobalEvents(JumpTo(InGameScene.name))

    case _ =>
      Outcome(loadingState)
  }

  override def eventFilters: EventFilters = EventFilters.AllowAll

  override def viewModelLens: Lens[IndigoViewModel, SceneViewModel] =
    Lens(_ => (), (viewModel, _) => viewModel)

  override def name: SceneName = SceneName("Loading")

  override def modelLens: Lens[IndigoModel, SceneModel] =
    Lens(_.loadingState, _.withLoadingState(_))

  override def updateViewModel(
      context: SceneContext[InGameScene.StartupData],
      model: SceneModel,
      viewModel: SceneViewModel
  ): GlobalEvent => Outcome[SceneViewModel] = _ => Outcome(())

}

object LoadingScene {
  type StartupData = Unit
}
