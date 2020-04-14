package utils.streams

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import io.circe.{Decoder, Encoder}
import io.circe.parser.decode

import akka.actor.typed.scaladsl.adapter._

object TypedActorFlow {

  private sealed trait EndOfStream
  private case object Success extends EndOfStream
  private case object Failure extends EndOfStream

  /**
    * Creates a flow from String to String that decodes incoming messages and send them to an actor.
    * Every Out message sent to the actor will be send downstream after being encoded by the Circe.
    * @param behavior describes the [[akka.actor.typed.Behavior]] receiving messages from upstream. It receives as
    *                 argument the actor to send messages to go downstream.
    * @param actorName unique for the internal actor in the flow
    * @param errorSink sink where decoding failures are sent
    */
  def stringActorRefCirce[In, Out](
      behavior: ActorRef[Out] => Behavior[In],
      actorName: String,
      errorSink: Sink[io.circe.Error, _] = Sink.ignore,
      bufferSize: Int                    = 16,
      overflowStrategy: OverflowStrategy = OverflowStrategy.dropNew
  )(implicit actorSystem: ActorSystem, inDecoder: Decoder[In], outEncoder: Encoder[Out]): Flow[String, String, _] = {
    val actorFlow = actorRef[In, Out](behavior, actorName, bufferSize, overflowStrategy)

    val fullErrorSink = Flow[Either[io.circe.Error, In]].collect { case Left(error) => error }.to(errorSink)

    Flow[String]
      .map(decode[In])
      .alsoTo(fullErrorSink)
      .collect { case Right(in) => in }
      .via(actorFlow)
      .map(outEncoder.apply)
      .map(_.noSpaces)
  }

  def actorRef[In, Out](
      behavior: ActorRef[Out] => Behavior[In],
      actorName: String,
      bufferSize: Int                    = 16,
      overflowStrategy: OverflowStrategy = OverflowStrategy.dropNew
  )(implicit actorSystem: ActorSystem): Flow[In, Out, _] = {
    val (outActor, publisher) = ActorSource
      .actorRef[Out](
        { case _ if false      => }: PartialFunction[Out, Unit],
        { case e: Any if false => new Exception(e.toString) }: PartialFunction[Any, Throwable],
        bufferSize,
        overflowStrategy
      )
      .toMat(Sink.asPublisher(false))(Keep.both)
      .run()

    val sink = Flow[In]
      .map(Right[EndOfStream, In])
      .to(
        ActorSink.actorRef[Either[EndOfStream, In]](
          actorSystem.spawn(
            Behaviors.setup[Either[EndOfStream, In]] { context =>
              val flowActor = context.spawn(behavior(outActor), "flowActor")
              context.watch(flowActor)

              Behaviors
                .receiveMessage[Either[EndOfStream, In]] {
                  case Right(in) =>
                    flowActor ! in
                    Behaviors.same
                  case Left(_) =>
                    context.stop(flowActor)
                    Behaviors.same
                }
                .receiveSignal {
                  case (_, Terminated(_)) =>
                    Behaviors.stopped
                }
            },
            actorName
          ),
          Left(Success),
          _ => Left(Failure)
        )
      )

    Flow.fromSinkAndSource(
      sink,
      Source.fromPublisher(publisher)
    )

  }

}
