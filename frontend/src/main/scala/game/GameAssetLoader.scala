package game

import assets.Asset
import assets.Asset.ingame.gui.bars._
import assets.Asset.ingame.gui.abilities._
import assets.Asset.ingame.gui.`default-abilities`._
import com.raquo.airstream.eventbus.EventBus
import game.GameAssetLoader.ProgressData
import typings.pixiJs.PIXI.LoaderResource
import typings.pixiJs.mod.Application
import typings.pixiJs.pixiJsStrings
import zio.{UIO, ZIO}
import org.scalablytyped.runtime.StringDictionary
import org.scalablytyped.runtime.StringDictionary.wrapStringDictionary

/**
  * The goal of the [[game.GameAssetLoader]] is simply to load all game assets, and warn the external world that it is
  * done, while also allowing to track the progress.
  */
final class GameAssetLoader(application: Application) {

  private val progressBus: EventBus[ProgressData] = new EventBus

  val assets: List[String] = List(
    xeonBar,
    liteStepBar,
    minimalistBar,
    abilityOverlay,
    hexagonFlashHeal,
    hexagonHot,
    squareTaunt,
    squareHammerHit,
    boss101BigDot,
    squareShield,
    rageFiller
  )

  final val $progressData = progressBus.events

  // todo: handle errors when loading
  val loadAssets: ZIO[Any, Nothing, PartialFunction[Asset, LoaderResource]] = for {
    fiber <- ZIO
      .effectAsync[Any, Nothing, StringDictionary[LoaderResource]] { callback =>
        assets
          .foldLeft(application.loader) { (loader, resourceUrl) =>
            loader.add(resourceUrl)
          }
          .load { (_, resources) =>
            callback(UIO(resources.asInstanceOf[StringDictionary[LoaderResource]]))
          }
          .on_progress(pixiJsStrings.progress, { (loader, resource) =>
            progressBus.writer.onNext(ProgressData(loader.progress, resource.name))
          })
      }
      .fork
    resources <- fiber.join
    fn <- UIO({
      case asset: Asset if resources.isDefinedAt(asset) => resources(asset)
    }: PartialFunction[Asset, LoaderResource])
  } yield fn

}

object GameAssetLoader {

  final case class ProgressData private (completion: Double, assetName: String)
  private object ProgressData {
    def apply(completion: Double, assetName: String): ProgressData = new ProgressData(completion, assetName)
  }

}
