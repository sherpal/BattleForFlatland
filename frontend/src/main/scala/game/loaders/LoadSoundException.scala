package game.loaders

import typings.std.MediaError

final class LoadSoundException(val mediaError: MediaError) extends RuntimeException(mediaError.message)
