package bffclient.hosting

import scala.concurrent.{ExecutionContext, Future}

import bffclient.controls.ClientSettings
import bffclient.menu.MenuApi
import menus.data.MenuGameComm

/** What a player's client does about the game server, from the moment it joins the game's lobby:
  * downloads what runs it ahead of time (if it can run it), then, when the game is launched (see
  * [[MenuGameComm]]), sends its candidacy to run it, and runs it when chosen. If it cannot, it says
  * so, and the menu server chooses someone else.
  *
  * Unless the player turned off hosting game servers in their settings ([[ClientSettings]]): then
  * nothing is downloaded, and the candidacy says so. Turning it back on in the lobby starts the
  * downloads (see [[prefetch]]); turning it off stops nothing that started already, but we are not
  * chosen.
  *
  * @param log
  *   where to tell what happens
  */
final class HostingAgent(api: MenuApi, gameId: String, userName: String, log: String => Unit)(using
    ExecutionContext
) {
  private var gameServer = Option.empty[Future[GameServerHost.GameServer]]

  private def willing: Boolean = ClientSettings.current.hostGameServers

  /** Starts downloading what runs the game server, if we may host it and did not start already (it
    * takes a while, better done before the game is launched). Called again by the lobby, every
    * frame, for when hosting gets turned on.
    */
  def prefetch(): Unit = if willing && gameServer.isEmpty then {
    val prepared = GameServerHost.prepare(api)
    prepared.failed.foreach(error => log(s"Cannot run the game server: ${error.getMessage}"))
    gameServer = Some(prepared)
  }

  prefetch()

  /** Who runs the game server, once chosen, to show while the game starts. */
  var status: Option[String] = None

  /** What is being downloaded to run the game server, if anything. */
  def downloading: Option[GameServerHost.Download] = GameServerHost.downloading

  def handle(message: MenuGameComm): Unit = message match {
    case MenuGameComm.HostCandidacyRequested() =>
      GameServerHost
        .candidacy(
          api,
          gameId,
          willing,
          ready = gameServer.exists(_.value.exists(_.isSuccess))
        )
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
      val prepared =
        if !willing then Future.failed(Exception("hosting game servers is turned off in the settings"))
        else
          // what failed to download in the lobby may work now
          gameServer.fold(GameServerHost.prepare(api))(_.recoverWith { case _ =>
            GameServerHost.prepare(api)
          })
      GameServerHost.spawn(api, prepared, gameId, gameSecret, rendezvousPort).failed.foreach {
        error =>
          log(s"Cannot run the game server: ${error.getMessage}")
          api.hostFailed(gameId, error.getMessage)
      }
    case _ => ()
  }
}
