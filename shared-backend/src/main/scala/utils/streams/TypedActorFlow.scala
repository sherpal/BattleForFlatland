package utils.streams

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import akka.stream.{Materializer, OverflowStrategy}
import io.circe.parser.decode
import io.circe.{Decoder, Encoder}

object TypedActorFlow {

  private sealed trait EndOfStream
  private case object Success extends EndOfStream
  private case object Failure extends EndOfStream

  /**
    * Creates a flow from String to String that decodes incoming messages and send them to an actor.
    * Every Out message sent to the actor will be sent downstream after being encoded by the Circe.
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

  /**
    * This is similar to `actorRef`, but it is created by a given ActorContext instead of the whole
    * ActorSystem. This was needed because akka typed would not let me create a typed actor when the
    * guardian is a custom behaviour.
    * @param behavior actor to create for ingesting flow income
    * @param actorName unique name to give to the actor
    * @param context ActorContext as given by a Behaviors.receiveMessage method
    */
  def actorRefFromContext[In, Out](
      behavior: ActorRef[Out] => Behavior[In],
      actorName: String,
      context: ActorContext[_],
      bufferSize: Int                    = 16,
      overflowStrategy: OverflowStrategy = OverflowStrategy.dropNew
  )(implicit materializer: Materializer): Flow[In, Out, _] = {
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
          context.spawn(
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

  /**
   * Creates a [[Flow]] where incoming elements (of type `In`) are sent to the given behaviour, and messages (of type
   * `Out`) sent to created actor go downstream.
   *
   * The `behavior` will receive an actor of type `Out`, and all message sent to it will go downstream.
   *
   * This is particularly useful for using in Play Websockets, where incoming messages from the client are handled by
   * the given behavior, and outgoing messages come from the actor.
   *
   * @param behavior Actor responsible to receive (and handle) elements of type In coming from upstream. This behavior
   *                 will receive as input the actor to which sent elements go downstream.
   * @param actorName name of the actor to spawn
   * @param bufferSize size of the upstream elements buffer
   * @param overflowStrategy strategy describing how to handle buffer overflow
   * @param actorSystem surrounding actor system
   * @tparam In type of elements/messages coming from upstream
   * @tparam Out type of elements/messages going downstream
   * @return a [[Flow]] from In to Out.
   */
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
