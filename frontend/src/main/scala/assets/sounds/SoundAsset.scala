package assets.sounds

import scala.collection.mutable
import urldsl.language.PathSegment
import urldsl.language.dummyErrorImpl.*
import assets.IndigoLikeAsset
import indigo.*

class SoundAsset private (val path: PathSegment[Unit, ?], val name: String)
    extends IndigoLikeAsset {

  override def asIndigoAssetType: AssetType = AssetType.Audio(assetName, AssetPath(pathStr))

  override final def equals(obj: Any): Boolean = obj match {
    case that: SoundAsset => this.name == that.name
    case _                => false
  }

  val assetName = indigo.AssetName(name)

  override final def hashCode(): Int = name.hashCode()

  private lazy val pathStr = "/" ++ path.createPart()

  def play(volume: Volume) = PlaySound(assetName, volume)

}

object SoundAsset {

  private val _allAssets = mutable.Set.empty[SoundAsset]

  def all: Set[SoundAsset] = _allAssets.toSet

  private def apply(path: PathSegment[Unit, ?]): SoundAsset = {
    val asset = new SoundAsset(
      path,
      path.createSegments().last.content.reverse.dropWhile(_ != '.').tail.reverse
    )
    _allAssets += asset
    asset
  }

  private val basePath = services.routing.base / "assets" / "sounds"

  object sounds {
    val pentagonBulletSound = SoundAsset(basePath / "penta-bullet.wav")
    val pentagonZoneSound   = SoundAsset(basePath / "penta-zone.wav")

    val hexagonHealHotSound = SoundAsset(basePath / "hexagon-heal-hot.wav")

    val triangleBigHitSound   = SoundAsset(basePath / "triangle-big-hit.wav")
    val triangleSmallHitSound = SoundAsset(basePath / "triangle-small-hit.wav")
  }

  sounds

}
