package game

import assets.Asset.ingame.gui.`default-abilities`._
import assets.Asset.ingame.gui.abilities._
import assets.Asset.ingame.gui.bars._
import assets.Asset.ingame.gui.boss.dawnOfTime.boss102._
import assets.Asset.ingame.gui.boss.dawnOfTime.boss103._
import assets.Asset.ingame.gui.markers._
import assets.{Asset, ScalaLogo}
import com.raquo.airstream.eventbus.EventBus
import game.GameAssetLoader.ProgressData
import org.scalablytyped.runtime.StringDictionary
import org.scalablytyped.runtime.StringDictionary.wrapStringDictionary
import typings.pixiJs.PIXI.LoaderResource
import typings.pixiJs.mod.Application
import typings.pixiJs.pixiJsStrings
import zio.{UIO, ZIO}
import typings.pixiJs.PIXI.Loader
import org.scalajs.dom
import scala.scalajs.js

/**
  * The goal of the [[game.GameAssetLoader]] is simply to load all game assets, and warn the external world that it is
  * done, while also allowing to track the progress.
  */
final class GameAssetLoader(application: Application) {

  private val progressBus: EventBus[ProgressData] = new EventBus
  private val endedBus: EventBus[Unit]            = new EventBus

  val assets: List[String] = List(
    ScalaLogo,
    xeonBar,
    liteStepBar,
    minimalistBar,
    abilityOverlay,
    hexagonFlashHeal,
    hexagonHot,
    squareTaunt,
    squareHammerHit,
    squareEnrage,
    squareCleave,
    triangleDirectHit,
    triangleUpgradeDirectHit,
    pentagonBullet,
    pentagonZone,
    pentagonDispel,
    boss101BigDot,
    squareShield,
    rageFiller,
    energyFiller,
    manaFiller,
    livingDamageZone,
    punished,
    purified,
    inflamed,
    sacredGroundArea,
    markerCross, markerLozenge, markerMoon, markerSquare, markerStar, markerTriangle
  )

  final val $progressData     = progressBus.events
  final val endedLoadingEvent = endedBus.events

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
          //.onProgress
          .on("progress", ({ ((loader: Loader), (resource: LoaderResource)) =>
            progressBus.writer.onNext(ProgressData(loader.progress, resource.name))
          }))//: js.Function2[typings.pixiJs.PIXI.Loader,typings.pixiJs.PIXI.LoaderResource, Unit]).asInstanceOf[scala.scalajs.js.Function1[scala.scalajs.js.Any, _]])
      }
      .fork
    resources <- fiber.join
    _         <- ZIO.effectTotal { endedBus.writer.onNext(()) }
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
