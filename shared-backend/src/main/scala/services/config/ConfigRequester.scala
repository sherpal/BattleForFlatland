package services.config

import com.typesafe.config.{Config, ConfigFactory}
import services.config.ConfigRequester.{FromConfig, PathDoesNotExistException}

import scala.concurrent.duration.Duration

/**
  * Represents a path in the configuration file.
  * In order to create the path "foo.bar", the way is to do
  * import ConfigRequester.|>
  * |> >> "foo" >> "bar"
  *
  * When the conf file is in "debug = true" mode, intermediate paths are checked to exist. So, in
  *   |> >> "foo" >> "bar" >> "dummy"
  * if the path "foo.bar" does not exist (and hence neither does "foo.bar.dummy"), you'll get a (runtime) error when
  * creating the "foo.bar" path, so that you already know that there was a problem there.
  *
  * @param path list of segments
  */
final class ConfigRequester private (path: Seq[String])(implicit debug: Boolean) {

  def isEmpty: Boolean = path.isEmpty

  def length: Int = path.length

  def previousPath: ConfigRequester = if (isEmpty) this else new ConfigRequester(path.dropRight(1))

  @scala.annotation.tailrec
  def maximalExistingSubPath: Option[ConfigRequester] =
    if (isEmpty) None else if (?) Some(this) else previousPath.maximalExistingSubPath

  def >>(segment: String): ConfigRequester = new ConfigRequester(path :+ segment)

  def >>>[T](fromConfig: FromConfig[T]): T = {
    if (debug) {
      maximalExistingSubPath match {
        case None => throw new PathDoesNotExistException(s"None of the sub-paths of $toString exist.")
        case Some(cr) if cr.length < this.length =>
          throw new PathDoesNotExistException(
            s"The maximal sub-path of $toString is ${cr.toString}."
          )
        case _ => // the path exists
      }
    }
    fromConfig(this)
  }
  def ?>>>[T](fromConfig: FromConfig[T]): Option[T] = if (?) Some(>>>(fromConfig)) else None

  def into[T](implicit fromConfig: FromConfig[T]): T              = >>>(fromConfig)
  def maybeInto[T](implicit fromConfig: FromConfig[T]): Option[T] = ?>>>(fromConfig)

  def ? : Boolean = ConfigRequester.config.hasPath(pathString)

  override def toString: String = path.mkString("|> ", " >> ", "")

  def pathString: String = path.mkString(".")

}

object ConfigRequester {

  private lazy val config: Config = ConfigFactory.load()

  private implicit lazy val debug: Boolean = config.getBoolean("debug")

  def |> : ConfigRequester = new ConfigRequester(Nil)

  sealed trait FromConfig[T] {
    def apply(configRequester: ConfigRequester): T
  }

  object FromConfig {

    implicit def booleanConfig: FromConfig[Boolean]   = BooleanConfig
    implicit def doubleConfig: FromConfig[Double]     = DoubleConfig
    implicit def inConfig: FromConfig[Int]            = IntConfig
    implicit def longConfig: FromConfig[Long]         = LongConfig
    implicit def stringConfig: FromConfig[String]     = StringConfig
    implicit def durationConfig: FromConfig[Duration] = DurationConfig

  }

  case object BooleanConfig extends FromConfig[Boolean] {
    def apply(configRequester: ConfigRequester): Boolean = config.getBoolean(configRequester.pathString)
  }
  case object DoubleConfig extends FromConfig[Double] {
    def apply(configRequester: ConfigRequester): Double = config.getDouble(configRequester.pathString)
  }
  case object IntConfig extends FromConfig[Int] {
    def apply(configRequester: ConfigRequester): Int = config.getInt(configRequester.pathString)
  }
  case object LongConfig extends FromConfig[Long] {
    def apply(configRequester: ConfigRequester): Long = config.getLong(configRequester.pathString)
  }
  case object StringConfig extends FromConfig[String] {
    def apply(configRequester: ConfigRequester): String = config.getString(configRequester.pathString)
  }
  case object DurationConfig extends FromConfig[Duration] {
    def apply(configRequester: ConfigRequester): Duration = Duration(config.getString(configRequester.pathString))
  }

  final class PathDoesNotExistException(path: String) extends Exception(s"Config path `$path` does not exist.")

}
