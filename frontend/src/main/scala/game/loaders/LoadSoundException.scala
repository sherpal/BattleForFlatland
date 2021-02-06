package game.loaders

import typings.std.MediaError

final class LoadSoundException(val mediaError: Option[MediaError])
    extends RuntimeException(mediaError.map(_.message).getOrElse("Unknown media error"))
