package assets

import indigo.*

trait IndigoLikeAsset {

  def assetName: AssetName

  def asIndigoAssetType: AssetType

}

object IndigoLikeAsset {

  def all: Set[IndigoLikeAsset] = Asset.all ++ sounds.SoundAsset.all

}
