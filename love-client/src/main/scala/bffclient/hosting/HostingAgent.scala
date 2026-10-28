package bffclient.hosting

import scala.concurrent.ExecutionContext

import bffclient.menu.MenuApi
import menus.data.MenuGameComm

/** What a player's client does about the game server, from the moment it joins the game's lobby:
  * downloads what runs it ahead of time (if it can run it), then, when the game is launched (see
  * [[MenuGameComm]]), sends its candidacy to run it, and runs it when chosen. If it cannot, it says
  * so, and the menu server chooses someone else.
  *
  * @param log
  *   where to tell what happens
  */
final class HostingAgent(api: MenuApi, gameId: String, userName: String, log: String => Unit)(using
    ExecutionContext
) {
  // right away, it takes a while
  private val gameServer = GameServerHost.prepare(api)
  gameServer.failed.foreach(error => log(s"Cannot run the game server: ${error.getMessage}"))

  /** Who runs the game server, once chosen, to show while the game starts. */
  var status: Option[String] = None

  /** What is being downloaded to run the game server, if anything. */
  def downloading: Option[GameServerHost.Download] = GameServerHost.downloading

  def handle(message: MenuGameComm): Unit = message match {
    case MenuGameComm.HostCandidacyRequested() =>
      GameServerHost
        .candidacy(api, gameId, ready = gameServer.value.exists(_.isSuccess))
        .flatMap { candidacy =>
          log(s"Candidacy to run the game server: $candidacy")
          api.hostCandidacy(candidacy)
        }
        .failed
        .foreach(error => log(s"Could not send our candidacy: ${error.getMessage}"))
    case MenuGameComm.HostElected(host) =>
      status = Some(
        if host == userName then "The game server runs on your computer"
        else s"The game server runs on the computer of $host"
      )
    case MenuGameComm.SpawnGameServer(_, gameSecret, rendezvousPort) =>
      // what failed to download in the lobby may work now
      val prepared = gameServer.recoverWith { case _ => GameServerHost.prepare(api) }
      GameServerHost.spawn(api, prepared, gameId, gameSecret, rendezvousPort).failed.foreach {
        error =>
          log(s"Cannot run the game server: ${error.getMessage}")
          api.hostFailed(gameId, error.getMessage)
      }
    case _ => ()
  }
}
