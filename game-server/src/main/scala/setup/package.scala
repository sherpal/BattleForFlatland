import zio.UIO

import services.database.gametables._
import zio.console.putStrLn

package object setup {

  def fetchGameInfo(gameId: String) =
    for {
      game <- gameWithPlayersById(gameId)
      _ <- putStrLn(game.toString)
    } yield ()

}
