package gamelogic.physics

import scala.language.implicitConversions
import scala.math._
import scala.util.Try

final case class Complex(re: Double, im: Double) {
  def +(other: Complex): Complex = Complex(re + other.re, im + other.im)

  def -(other: Complex): Complex = Complex(re - other.re, im - other.im)

  def *(other: Complex): Complex = Complex(re * other.re - im * other.im, re * other.im + im * other.re)

  def /(other: Complex): Complex = {
    val d = other.re * other.re + other.im * other.im
    Complex((re * other.re + im * other.im) / d, (-re * other.im + im * other.re) / d)
  }

  def **(other: Int): Complex = {

    def pow(base: Complex, exponent: Int): Complex = exponent match {
      case 0 => Complex(1, 0)
      case 1 => this
      case n if n < 0 =>
        1 / pow(base, exponent)
      case _ =>
        val subResult = pow(base, exponent / 2)
        if (exponent % 2 == 0) subResult * subResult
        else subResult * subResult * this
    }

    pow(this, other)
  }

  def **(other: Double): Complex = Complex.exp(other * Complex.log(this))

  def **(other: Complex): Complex = Complex.exp(other * Complex.log(this))

  /** Scalar product. */
  @inline def |*|(that: Complex): Double = this.re * that.re + this.im * that.im

  @inline def crossProduct(that: Complex): Double = this.re * that.im - this.im * that.re

  @inline def scalarProduct(that: Complex): Double = this |*| that

  def multiply(seq: Seq[Complex]): Seq[Complex] = for (z <- seq) yield this * z

  def modulus: Double = sqrt(re * re + im * im)

  def modulus2: Double = re * re + im * im

  def orthogonal: Complex = Complex(-im, re)

  def normalized: Complex = this / modulus

  def arg: Double = atan2(im, re) // arg in (-pi, pi)

  def unary_- : Complex = Complex(-re, -im)

  def unary_~ : Complex = Complex(re, -im)

  def unary_! : Double = modulus

  override def equals(that: Any): Boolean = that match {
    case that: Complex => math.max(math.abs(that.re - re), math.abs(that.im - im)) < 1e-6
    case _             => false
  }

  override def hashCode(): Int = re.## ^ im.##

  override def toString: String = this match {
    case Complex.i               => "1 im"
    case Complex(r, 0)           => r.toString
    case Complex(0, i)           => i.toString + " im"
    case Complex(r, i) if i >= 0 => r.toString + " + " + i.toString + " im"
    case Complex(r, i)           => r.toString + " - " + math.abs(i).toString + " im"
  }
}

object Complex {

  def apply(z: (Double, Double)): Complex = Complex(z._1, z._2)

  final val i = Complex(0, 1)

  private val rnd: java.util.Random = new java.util.Random()

  def rndComplex(): Complex = Complex(rnd.nextDouble(), rnd.nextDouble())

  implicit def fromDouble(d: Double): Complex          = Complex(d, 0)
  implicit def fromInt(n: Int): Complex                = Complex(n, 0)
  implicit def fromTuple(z: (Double, Double)): Complex = Complex(z._1, z._2)

  def exp(z: Complex): Complex = math.exp(z.re) * Complex(math.cos(z.im), math.sin(z.im))

  def log(z: Complex): Complex = Complex(math.log(!z), z.arg) // principal branch of log

  def log(z: Complex, branch: Double): Complex = Complex(math.log(!z), z.arg + (if (z.arg < branch) 2 * Pi else 0))

  def sqrt(z: Complex): Complex = exp(log(z) / 2)

  def pow(z: Complex, r: Complex): Complex = exp(r * log(z))

  def sin(z: Complex): Complex = (exp(i * z) - exp(-i * z)) / (2 * i)

  def cos(z: Complex): Complex = (exp(i * z) + exp(-i * z)) / 2

  def rotation(angle: Double): Complex = Complex(math.cos(angle), math.sin(angle))

  implicit object ComplexIsNumeric extends Numeric[Complex] {
    override def plus(x: Complex, y: Complex): Complex = x + y

    override def minus(x: Complex, y: Complex): Complex = x - y

    override def times(x: Complex, y: Complex): Complex = x * y

    override def negate(x: Complex): Complex = -x

    override def fromInt(x: Int): Complex = Complex(x, 0)

    override def toInt(x: Complex): Int = x.re.toInt

    override def toLong(x: Complex): Long = x.re.toLong

    override def toFloat(x: Complex): Float = x.re.toFloat

    override def toDouble(x: Complex): Double = x.re

    override def compare(x: Complex, y: Complex): Int = java.lang.Double.compare(!x, !y)

    // todo: also write a regex that matches the toString method
    def parseString(str: String): Option[Complex] = {
      val values = str.drop(8).dropRight(1).split(",").map(s => Try(s.toDouble).toOption)
      for {
        real <- values(0)
        imag <- values(1)
      } yield Complex(real, imag)
    }
  }
}
