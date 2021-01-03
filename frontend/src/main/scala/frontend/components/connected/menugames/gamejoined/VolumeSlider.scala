package frontend.components.connected.menugames.gamejoined

import com.raquo.laminar.api.L._
import assets.sounds.Volume
import utils.laminarzio.Implicits._
import utils.laminarzio.onMountZIO
import frontend.components.utils.Slider

import zio._
import scala.concurrent.Future

object VolumeSlider {

  def apply() = {

    val volume = Var(Volume.full)

    Slider(
      volume.signal.map(_.value),
      volume.writer.contramap(Volume(_))
    ).amend(
      onMountZIO(
        Volume.loadStoredVolume
          .tap(v => ZIO.effectTotal(volume.set(v)))
          .unit
      ),
      volume.signal.changes.flatMap(Volume.storeVolume(_)) --> Observer.empty
    )
  }

}
