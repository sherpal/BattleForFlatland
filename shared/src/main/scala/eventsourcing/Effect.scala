package eventsourcing

enum Effect[Event, State] {

  /** Will persist the given event to the database, before doing anything else. */
  case Persist[E, S](event: E) extends Effect[E, S]

  /** Will persist all the specified events, from left to right. */
  case PersistMultiple[E, S](events: Vector[E]) extends Effect[E, S]

  /** Will first apply the effect on the left, then runs a side effect with the new state. */
  case WithSideEffect[E, S](effect: Effect[E, S], sideEffect: S => Unit) extends Effect[E, S]

  /** Does nothing. */
  case Ignore[E, S]() extends Effect[E, S]

  /** Stateless effect that simply replies to the specified actor. */
  case ReplyTo[E, S, Message](replyTo: castor.Actor[Message], message: S => Message) extends Effect[E, S]

  /** Will first apply this effect, then will run the side effect on the new state.
    * @param f
    *   side effect to run on the new entity state
    */
  def thenRun(f: State => Unit): Effect[Event, State] = WithSideEffect(this, f)

  /** Will first apply this effect, then will send a message to the specified actor. */
  def thenReply[Message](to: castor.Actor[Message])(message: State => Message): Effect[Event, State] =
    thenRun(state => to.send(message(state)))

}
