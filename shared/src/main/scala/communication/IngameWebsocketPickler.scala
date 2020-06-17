package communication

import boopickle.CompositePickler
import boopickle.Default._
import gamelogic.physics.Complex

object IngameWebsocketPickler {

  trait A

  case class B(x: Complex) extends A
  case class C(s: String) extends A

  trait X
  case class Y(y: Int, a: A) extends X

  implicit val aPickler = CompositePickler[A]
    .addConcreteType[B]
    .addConcreteType[C]

  val pickled = Pickle.intoBytes(Y(3, B(Complex(1, 2))))

  println(pickled)

  val x = Unpickle.apply[Y].fromBytes(pickled)

  println(x)

}
