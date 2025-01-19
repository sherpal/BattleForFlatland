package application

import models.bff.ingame.InGameWSProtocol
import models.bff.outofgame.gameconfig.PlayerInfo

final case class ConnectedPlayerInfo(
    send: InGameWSProtocol => Unit,
    playerInfo: PlayerInfo,
    isReady: Boolean
) {
  def userName = playerInfo.playerName.name
}
