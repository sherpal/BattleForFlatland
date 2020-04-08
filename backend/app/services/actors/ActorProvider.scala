package services.actors

import akka.actor.ActorRef
import zio._

object ActorProvider {

  /**
    * This service can provide reference to actors by their names.
    */
  trait Service {
    def actors: UIO[Map[String, ActorRef]]
  }

  type ActorProvider = Has[ActorProvider.Service]

  def actorRef(name: String): URIO[ActorProvider, Option[ActorRef]] = ZIO.accessM(_.get.actors.map(_.get(name)))

  def live(_actors: Map[String, ActorRef]): Layer[Nothing, Has[Service]] = ZLayer.succeed(
    new Service {
      def actors: UIO[Map[String, ActorRef]] = UIO(_actors)
    }
  )

}
