import org.scalacheck.Prop.forAll
import org.scalacheck._
import org.scalacheck.util.Pretty

package object testutils {

  def functionEquality[T, U](gen: Gen[T])(f1: T => U, f2: T => U)(
      implicit
      s1: Shrink[T],
      pp1: T => Pretty
  ): Prop = forAll(gen) { t =>
    f1(t) == f2(t)
  }

}
