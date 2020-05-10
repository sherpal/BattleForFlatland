package game

import assets.ingame.gui.bars.{LiteStepBar, XeonBar}
import com.raquo.airstream.eventbus.EventBus
import game.GameAssetLoader.ProgressData
import typings.pixiJs.PIXI.LoaderResource
import typings.pixiJs.mod.{Application, Loader}
import typings.pixiJs.pixiJsStrings
import zio.{UIO, ZIO}

/**
  * The goal of the [[game.GameAssetLoader]] is simply to load all game assets, and warn the external world that it is
  * done, while also allowing to track the progress.
  */
final class GameAssetLoader(application: Application) {

  private val progressBus: EventBus[ProgressData] = new EventBus

  val assets: List[String] = List(
    XeonBar,
    LiteStepBar
  )

  final val $progressData = progressBus.events

  val loadAssets: ZIO[Any, Nothing, Map[String, LoaderResource]] = for {
    fiber <- ZIO
      .effectAsync[Any, Nothing, Map[String, LoaderResource]] { callback =>
        assets
          .foldLeft(application.loader) { (loader, resourceUrl) =>
            loader.add(resourceUrl)
          }
          .load { (_, resources) =>
            callback(UIO(resources.toMap))
          }
          .on_progress(pixiJsStrings.progress, { (loader, resource) =>
            progressBus.writer.onNext(ProgressData(loader.progress, resource.name))
          })
      }
      .fork
    result <- fiber.join
  } yield result

}

object GameAssetLoader {

  final case class ProgressData private (completion: Double, assetName: String)
  private object ProgressData {
    def apply(completion: Double, assetName: String): ProgressData = new ProgressData(completion, assetName)
  }

}
