package gamelogic.physics

import org.scalacheck._
import org.scalacheck.Prop._
import org.scalacheck.Prop.propBoolean
import testutils._

object ComplexChecks extends Properties("Complex properties") {

  property("Complex sum is commutative") = forAll(complexGen, complexGen) { (z1, z2) =>
    z1 + z2 == z2 + z1
  }
  property("0 is neutral for sum") = functionEquality(complexGen)(_ + 0, identity)
  property("Complex have inverse for sum") = forAll(complexGen) { z =>
    z + (-z) == (0: Complex)
  }
  property("Complex product is commutative") = forAll(complexGen, complexGen) { (z1, z2) =>
    z1 * z2 == z2 * z1
  }
  property("1 is neutral for product") = functionEquality(complexGen)(_ * 1, identity)

  property("Non zero Complex have inverse for product") = forAll(complexGen.filter(_.modulus > 0)) { z =>
    z * (1 / z) == (1: Complex)
  }
  property("Distributivity") = forAll(complexGen, complexGen, complexGen) { (z, w, u) =>
    z * (w + u) == z * w + z * u
  }

  property("z / w is the same as 1/(w/z)") =
    forAll(complexGen.filter(_.modulus > 0), complexGen.filter(_.modulus > 0)) { (z, w) =>
      z / w == 1 / (w / z)
    }

  property("Rotating Complex doesn't change its norm") = forAll(complexGen, angleGen) { (z, alpha) =>
    z.modulus.almostEqual(z.rotate(alpha).modulus)
  }

  property("Complex times conjugate is modulus square") =
    functionEquality(complexGen)(z => z * z.conjugate, _.modulus2: Complex)

  property("Complex conjugate is an involution") = functionEquality(complexGen)(_.conjugate.conjugate, identity)

  property("Applying four times the orthogonal is identity") = functionEquality(complexGen)(
    (1 to 4).foldLeft(_)((w, _) => w.orthogonal),
    identity
  )

  property("Orthogonal Complex has same norm, is orthogonal and gives positive cross product") = forAll(complexGen) {
    z =>
      val orthogonal = z.orthogonal
      all(
        z.modulus == orthogonal.modulus,
        (z scalarProduct orthogonal) == 0,
        (z crossProduct orthogonal) > 0
      )

  }

}
