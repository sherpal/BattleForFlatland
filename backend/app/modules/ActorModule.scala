package modules

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport
import websocketkeepers.gameantichamber.JoinedGameDispatcher
import websocketkeepers.gamemenuroom.GameMenuRoomBookKeeper

final class ActorModule extends AbstractModule with AkkaGuiceSupport {

  override def configure(): Unit = {
    bindActor[GameMenuRoomBookKeeper](GameMenuRoomBookKeeper.name)
    bindActor[JoinedGameDispatcher](JoinedGameDispatcher.name)
  }
}
