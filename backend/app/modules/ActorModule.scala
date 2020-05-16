package modules

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport
import websocketkeepers.gameantichamber.{JoinedGameDispatcherTyped}
import websocketkeepers.gamemenuroom.{GameMenuRoomBookKeeper, GameMenuRoomBookKeeperTyped}

final class ActorModule extends AbstractModule with AkkaGuiceSupport {

  /**
    * If you mess up and bind the same name twice, chances are you'll need to `clean` the compiled files
    * in order for it to work again!
    */
  override def configure(): Unit = {
    bindActor[GameMenuRoomBookKeeper](GameMenuRoomBookKeeper.name)

    bindTypedActor(
      GameMenuRoomBookKeeperTyped,
      GameMenuRoomBookKeeper.name + "typed"
    )
    bindTypedActor(
      JoinedGameDispatcherTyped,
      JoinedGameDispatcherTyped.name
    )

  }
}
