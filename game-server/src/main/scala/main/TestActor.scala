package main

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import models.bff.ingame.InGameWSProtocol

object TestActor {

  def apply(outerWorld: ActorRef[InGameWSProtocol]): Behavior[InGameWSProtocol] = Behaviors.receiveMessage { message =>
    outerWorld ! message
    Behaviors.same
  }

}
