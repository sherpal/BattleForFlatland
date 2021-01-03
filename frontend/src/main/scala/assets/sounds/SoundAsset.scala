package assets.sounds

import urldsl.language.PathSegment
import urldsl.language.PathSegment.dummyErrorImpl._
import urldsl.errors.DummyError
import cats.data.NonEmptyList
import assets.sounds.SoundFileExtension.Wav
import gamelogic.abilities.Ability
import assets.sounds.SoundFileExtension.Mp3
import gamelogic.abilities.triangle._
import gamelogic.abilities.hexagon._
import gamelogic.abilities.pentagon._
import gamelogic.abilities.square._

/**
  * Represents a loadable sound file. Instances of this trait will be used to load
  * all game sounds before the game start, and then easily run any of these sounds.
  *
  * @tparam For represents the type of object this sound is downloaded for. For example,
  *             If the sound is intended to be played for an ability, we could have
  *             `For =:= Ability`. It is contravariant because if you have a [[SoundAsset]]
  *             for `Any`, you can in principle use it for `Ability`.
  */
sealed trait SoundAsset[-For] {

  /**
    * Path of the directory where the [[SoundAsset]] can be found.
    */
  val filePath: PathSegment[Unit, DummyError]

  /**
    * In order list of file extension to try in order to load the sound file.
    * For exemple, if
    * {{{
    *   val extensions = List(SoundFileExtension.Wav, SoundFileExtension.Mp3)
    * }}}
    * it will first try to load the file with extension ".wav" and if that does not
    * work, it will try ".mp3".
    */
  val extensions: NonEmptyList[SoundFileExtension]

  def nextExtension: Option[SoundAsset[For]] = extensions.tail match {
    case Nil            => None
    case second :: rest => Some(SoundAsset[For](filePath, filename, second, rest: _*))
  }

  /**
    * Filename without the extension
    */
  val filename: String

  def filePathWithNameAndExt: PathSegment[(String, SoundFileExtension), DummyError] =
    filePath / SoundAsset.fileNameAndExt

  def possibleNames: NonEmptyList[String] = extensions.map(ext => filePathWithNameAndExt.createPath((filename, ext)))

  /**
    * Returns the url corresponding to the first extension.
    *
    * Taking the head is safe since extensions is a [[NonEmptyList]].
    */
  def url: String = filePathWithNameAndExt.createPath((filename, extensions.head))

}

object SoundAsset {

  private def apply[For](
      filePath0: PathSegment[Unit, DummyError],
      filename0: String,
      extension0: SoundFileExtension,
      otherExtensions: SoundFileExtension*
  ): SoundAsset[For] = new SoundAsset[For] {
    val filePath: PathSegment[Unit, DummyError]      = filePath0
    val filename: String                             = filename0
    val extensions: NonEmptyList[SoundFileExtension] = NonEmptyList(extension0, otherExtensions.toList)
  }

  private val fileNameAndExt = segment[String].as(
    { (str: String) =>
      val parts    = str.split('.')
      val ext      = SoundFileExtension.fromExt(parts.last).get
      val filename = parts.dropRight(1).mkString(".")
      (filename, ext)
    },
    ((filename: String, ext: SoundFileExtension) => filename ++ "." ++ ext.ext).tupled
  )

  val basePath = root / "assets" / "in-game" / "sounds"

  /** Marker trait for sounds emitted by general events in the game. */
  sealed trait GeneralGameEffectSound

  val gameOverSoundAsset = SoundAsset[GeneralGameEffectSound](basePath, "game-over", Wav)
  val bossDefeated       = SoundAsset[GeneralGameEffectSound](basePath, "boss-defeated", Wav)
  object abilities {

    val abilityPath = basePath / "abilities"

    object hexagon {
      val hexagonAbilitiesPath = abilityPath / "hexagon"

      val flashHeal  = SoundAsset[FlashHeal](hexagonAbilitiesPath, "flash-heal", Wav)
      val hexagonHot = SoundAsset[HexagonHot](hexagonAbilitiesPath, "hexagon-hot", Wav, Mp3)
    }

    object pentagon {
      val pentagonAbilitiesPath = abilityPath / "pentagon"

      val createPentagonBullet =
        SoundAsset[CreatePentagonBullet](pentagonAbilitiesPath, "create-pentagon-bullet", Wav, Mp3)
      val createPentagonZone =
        SoundAsset[CreatePentagonZone](pentagonAbilitiesPath, "create-pentagon-zone", Wav, Mp3)
      val pentaDispel = SoundAsset[PentaDispel](pentagonAbilitiesPath, "penta-dispel", Wav, Mp3)
    }

    object triangle {

      val triangleAbilitiesPath = abilityPath / "triangle"

      val triangleDirectHit        = SoundAsset[DirectHit](triangleAbilitiesPath, "direct-hit", Mp3)
      val triangleEnergyKick       = SoundAsset[EnergyKick](triangleAbilitiesPath, "energy-kick", Wav)
      val triangleUpgradeDirectHit = SoundAsset[UpgradeDirectHit](triangleAbilitiesPath, "upgrade-direct-hit", Wav)
      val triangleStun             = SoundAsset[Stun](triangleAbilitiesPath, "stun", Wav)
    }
  }

  val abilitySounds: Map[Ability.AbilityId, SoundAsset[_ <: Ability]] = Map(
    Ability.hexagonFlashHealId -> abilities.hexagon.flashHeal,
    Ability.hexagonHexagonHotId -> abilities.hexagon.hexagonHot,
    Ability.pentagonPentagonBullet -> abilities.pentagon.createPentagonBullet,
    Ability.createPentagonZoneId -> abilities.pentagon.createPentagonZone,
    Ability.pentagonDispelId -> abilities.pentagon.pentaDispel,
    Ability.triangleDirectHit -> abilities.triangle.triangleDirectHit,
    Ability.triangleEnergyKick -> abilities.triangle.triangleEnergyKick,
    Ability.triangleUpgradeDirectHit -> abilities.triangle.triangleUpgradeDirectHit,
    Ability.triangleStun -> abilities.triangle.triangleStun
  )

  val allSoundAssets: List[SoundAsset[_]] = List(
    gameOverSoundAsset,
    bossDefeated
  ) ++ abilitySounds.values

}
