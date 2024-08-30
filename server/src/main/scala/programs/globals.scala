package programs

import zio.*
import services.*
import menus.data.User
import services.events.Event.UserConnectedSocket
import services.events.Event.UserSocketDisconnected

val userConnectedWatcher = {
  case class UserData(user: User, gameId: String, howManyTimesWeSawThemDisconnected: Int = 0) {
    def isConnected: Boolean = howManyTimesWeSawThemDisconnected == 0

    def againSeenDisconnected: UserData =
      copy(howManyTimesWeSawThemDisconnected = howManyTimesWeSawThemDisconnected + 1)
  }
  for {
    connectedUserRef <- Ref.make(Map.empty[(User, String), UserData])
    _ <- events.registerEvents[UserConnectedSocket](ZIO.succeed(true)) {
      case UserConnectedSocket(user, gameId) =>
        connectedUserRef
          .update(_ + ((user, gameId) -> UserData(user, gameId))) *> Console
          .printLine(s"Connection from ${user.name}")
          .ignore
    }
    _ <- events.registerEvents[UserSocketDisconnected](ZIO.succeed(true)) {
      case UserSocketDisconnected(user, gameId) =>
        Console.printLine(s"${user.name} socket is disconnected from $gameId").ignore *>
          connectedUserRef.update(_.updatedWith((user, gameId)) {
            case None           => Some(UserData(user, gameId, 1))
            case Some(userData) => Some(userData.againSeenDisconnected)
          })
    }
    connectionCheck = for {
      currentGames   <- menugames.menuGames
      connectedUsers <- connectedUserRef.get
      _ <- ZIO.foreach(
        connectedUsers.keys.filterNot((user, _) => currentGames.exists(_.containsPlayer(user)))
      )(key => connectedUserRef.update(_ - key))
      _ <- ZIO.foreach(currentGames)(game =>
        for {
          _ <- ZIO.foreach(game.players)(player =>
            connectedUsers.get((player, game.id)) match {
              case None => events.dispatchEvent(UserSocketDisconnected(player, game.id))
              case Some(userData) =>
                userData.howManyTimesWeSawThemDisconnected match {
                  case 0 => // user is connected, nothing to do
                    ZIO.unit
                  case 1 => // user was seen disconnected once, let's give them another chance
                    events.dispatchEvent(UserSocketDisconnected(player, game.id)) *> Console
                      .printLine(s"${player.name} was seen disconnected once")
                      .ignore
                  case n => // seen too many times disconnected, let's remove them
                    val removingPlayer =
                      if game.isGameCreator(player) then menugames.deleteGame(game.id)
                      else menugames.removePlayer(player, game.id)
                    val dispatchingUpdate =
                      events.dispatchEvent(events.Event.GameDataRefreshed(Some(game.id)))
                    val removingUserFromTheList = connectedUserRef.update(_ - ((player, game.id)))

                    removingPlayer *> dispatchingUpdate *> removingUserFromTheList *> Console
                      .printLine(
                        s"${player.name} was disconnected too long and got kicked from ${game.id}"
                      )
                      .ignore
                }
            }
          )
        } yield ()
      )
    } yield ()
    _ <- Clock.sleep(10.seconds)
    _ <- connectionCheck.repeat(Schedule.spaced(30.seconds))
  } yield ()
}
