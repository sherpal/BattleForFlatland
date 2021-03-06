package game.loaders

import zio._
import typings.std.global.Audio
import typings.std.EventListenerOrEventListenerObject
import org.scalajs.dom
import assets.sounds.SoundAsset
import typings.std.MediaError
import org.scalajs.dom.raw.ErrorEvent

import scala.scalajs.js
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream
import scala.concurrent.Future
import services.logging._
import assets.sounds.Volume
import scala.util.Try

final class SoundAssetLoader(assets: List[SoundAsset[_]]) {

  private def loadSound(soundAsset: SoundAsset[_], globalVolume: Volume): ZIO[Any, Option[MediaError], Audio] =
    for {
      _ <- ZIO.unit
      audio = new Audio(soundAsset.url)
      fiber <- ZIO
        .effectAsync[Any, Option[MediaError], Unit] { cb =>
          audio.addEventListener_canplaythrough(typings.std.stdStrings.canplaythrough, { (_: Audio, _: dom.Event) =>
            cb(UIO(()))
          })

          audio.addEventListener_error(
            typings.std.stdStrings.error, { (_: Audio, error: ErrorEvent) =>
              cb(
                ZIO.fail(
                  Try(
                    error
                      .asInstanceOf[js.Dynamic]
                      .path
                      .asInstanceOf[js.Array[js.Object]](0)
                      .asInstanceOf[js.Dynamic]
                      .error
                      .asInstanceOf[MediaError]
                  ).toOption
                )
              )
            }
          )
        }
        .fork
      _ <- fiber.join
      _ <- ZIO.effectTotal {
        audio.volume = globalVolume.value
      }
    } yield audio

  private def loadSoundRetry(soundAsset: SoundAsset[_], globalVolume: Volume): ZIO[Any, LoadSoundException, Audio] =
    loadSound(soundAsset, globalVolume)
      .mapError(mediaError => (soundAsset.nextExtension, mediaError))
      .catchAll {
        // trying next extention
        case (Some(nextExtention), _) => loadSoundRetry(nextExtention, globalVolume)
        // exhausted all extensions, failing with last error
        case (_, mediaError) => ZIO.fail(new LoadSoundException(mediaError))
      }

  private val progressBus: EventBus[SoundAsset[_]]                = new EventBus
  private val endedBus: EventBus[SoundAssetLoader.LoadEnded.type] = new EventBus
  private val startedBus: EventBus[SoundAssetLoader.Started.type] = new EventBus

  private def emitLoading(asset: SoundAsset[_]) =
    ZIO.effectTotal {
      progressBus.writer.onNext(asset)
    }

  private val assetCount = assets.length

  private val progressionEvents = progressBus.events
    .fold((1, Option.empty[SoundAsset[_]])) {
      case ((lastCount, _), nextAsset) =>
        (lastCount + 1, Some(nextAsset))
    }
    .changes
    .collect {
      case (count, Some(asset)) =>
        SoundAssetLoader.OngoingProgressData(
          count * 100.0 / assetCount,
          asset
        )
    }

  /**
    * You need to run this effect in order to load all sound assets
    */
  def loadingEffect(globalVolume: Volume): IO[LoadSoundException, Map[SoundAsset[_], Audio]] =
    for {
      _ <- ZIO.effectTotal(startedBus.writer.onNext(SoundAssetLoader.Started))
      allLoadedAudio <- ZIO
        .foreachParN(3)(assets) { asset =>
          loadSoundRetry(asset, globalVolume) <* emitLoading(asset)
        }
        .tapError(error => ZIO.effectTotal(endedBus.writer.onError(_)))
      assetToAudioMap = assets.zip(allLoadedAudio).toMap
      _ <- ZIO.effectTotal {
        endedBus.writer.onNext(SoundAssetLoader.LoadEnded)
      }
    } yield assetToAudioMap

  /**
    * Tries to load all [[Audio]]s from the list of [[SoundAsset]], and creates
    * a Map from the sound asset to maybe the [[Audio]], depending on whether the
    * loading was succesful.
    *
    * Log warnings in case it fails.
    */
  def maybeLoadingEffect(globalVolume: Volume): ZIO[Logging, Nothing, Map[SoundAsset[_], Option[Audio]]] =
    for {
      _ <- ZIO.effectTotal(startedBus.writer.onNext(SoundAssetLoader.Started))
      allLoadedAudio <- ZIO
        .foreachParN(3)(assets) { asset =>
          for {
            audioOrFail <- loadSoundRetry(asset, globalVolume).either
            _           <- emitLoading(asset)
          } yield audioOrFail
        }
      assetWithAudioEither = assets.zip(allLoadedAudio)
      assetToAudioMap      = assetWithAudioEither.map { case (asset, maybeAudio) => (asset, maybeAudio.toOption) }.toMap
      assetFailed          = assetWithAudioEither.collect { case (asset, Left(_)) => asset.filename }
      _ <- ZIO.when(assetFailed.nonEmpty)(
        log.warn(s"The following asset could not be loaded: ${assetFailed.mkString(", ")}.")
      )
      _ <- ZIO.effectTotal {
        endedBus.writer.onNext(SoundAssetLoader.LoadEnded)
      }
    } yield assetToAudioMap

  /**
    * Returns a Map from the [[SoundAsset]] to their corresponding [[Audio]], but only
    * for the one who succeeded to load.
    */
  def onlySuccessLoadingEffect(globalVolume: Volume): ZIO[Logging, Nothing, Map[SoundAsset[_], Audio]] =
    for {
      map <- maybeLoadingEffect(globalVolume)
      trimmedMap = map.collect { case (asset, Some(audio)) => (asset, audio) }
    } yield trimmedMap

  val allProgressionEvents: EventStream[SoundAssetLoader.ProgressData] = EventStream.merge(
    startedBus.events,
    progressionEvents,
    endedBus.events
  )

}

object SoundAssetLoader {

  // todo[scala3] replace with enum
  sealed trait ProgressData {
    def progression: Double

    final def ended: Boolean = progression == 100.0
  }

  case object Started extends ProgressData {
    def progression: Double = 0.0
  }

  /**
    * Allows to track progression for sound asset loading.
    *
    * @param progression percentage (in [0, 100] range.)
    * @param asset asset that was just loaded.
    */
  final case class OngoingProgressData(progression: Double, asset: SoundAsset[_]) extends ProgressData

  /**
    * Emitted when the load has ended
    */
  case object LoadEnded extends ProgressData {
    def progression: Double = 100.0
  }

}
