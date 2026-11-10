package programs

import menus.data.User
import server.BackendEnv
import services.events.Event

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{Executors, TimeUnit}

/** Runs `body` on its own (daemon) thread, so that it can block. */
private[programs] def fork(name: String)(body: => Unit): Unit = {
  val thread = Thread(
    () =>
      try body
      catch { case e: Throwable => e.printStackTrace() },
    name
  )
  thread.setDaemon(true)
  thread.start()
}

/** Every 10 seconds, kicks the players of the games who are not connected anymore (they get a
  * second chance first, see [[server.LobbyServer]] for how they are seen connected).
  */
def userConnectedWatcher(using env: BackendEnv): Unit = {
  case class UserData(user: User, gameId: String, howManyTimesWeSawThemDisconnected: Int = 0) {
    def isConnected: Boolean = howManyTimesWeSawThemDisconnected == 0

    def againSeenDisconnected: UserData =
      copy(howManyTimesWeSawThemDisconnected = howManyTimesWeSawThemDisconnected + 1)
  }
  val connectedUserRef = AtomicReference(Map.empty[(User, String), UserData])
  env.events.registerEvents[Event.UserConnectedSocket](true) {
    case Event.UserConnectedSocket(user, gameId) =>
      connectedUserRef.updateAndGet(_ + ((user, gameId) -> UserData(user, gameId)))
  }
  env.events.registerEvents[Event.UserSocketDisconnected](true) {
    case Event.UserSocketDisconnected(user, gameId) =>
      connectedUserRef.updateAndGet(_.updatedWith((user, gameId)) {
        case None => Some(UserData(user, gameId, 1))
        case Some(userData) => Some(userData.againSeenDisconnected)
      })
  }
  def connectionCheck(): Unit = {
    val currentGames = env.menuGames.menuGames
    // todo: what to do with started games?
    val connectedUsers = connectedUserRef.get
    connectedUsers.keys
      .filterNot((user, _) => currentGames.exists(_.containsPlayer(user)))
      .foreach(key => connectedUserRef.updateAndGet(_ - key))
    for {
      game   <- currentGames
      player <- game.players
    } connectedUsers.get((player, game.id)) match {
      case None => env.events.dispatchEvent(Event.UserSocketDisconnected(player, game.id))
      case Some(userData) =>
        userData.howManyTimesWeSawThemDisconnected match {
          case 0 => // user is connected, nothing to do
          case 1 => // user was seen disconnected once, let's give them another chance
            env.events.dispatchEvent(Event.UserSocketDisconnected(player, game.id))
            println(s"${player.name} was seen disconnected once")
          case _ => // seen too many times disconnected, let's remove them
            if game.isGameCreator(player) then env.menuGames.deleteGame(game.id)
            else env.menuGames.removePlayer(player, game.id)
            env.events.dispatchEvent(Event.GameDataRefreshed(Some(game.id)))
            connectedUserRef.updateAndGet(_ - ((player, game.id)))
            println(s"${player.name} was disconnected too long and got kicked from ${game.id}")
        }
    }
  }
  val checker = Executors.newSingleThreadScheduledExecutor { runnable =>
    val thread = Thread(runnable, "connection-check")
    thread.setDaemon(true)
    thread
  }
  checker.scheduleWithFixedDelay(
    () =>
      try connectionCheck()
      catch { case e: Throwable => e.printStackTrace() },
    10,
    10,
    TimeUnit.SECONDS
  )
}
