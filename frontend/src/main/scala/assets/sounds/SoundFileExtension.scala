package assets.sounds

/**
  * Represents a possible (aka supported) file extension for sound files.
  */
sealed trait SoundFileExtension {
  def ext: String
}

object SoundFileExtension {
  // todo[scala3] replace with enum

  case object Wav extends SoundFileExtension { def ext: String = "wav" }
  case object Mp3 extends SoundFileExtension { def ext: String = "mp3" }

  def allExtensiions = List(Wav, Mp3)

  def fromExt(extension: String): Option[SoundFileExtension] =
    allExtensiions.find(_.ext == extension)

}
