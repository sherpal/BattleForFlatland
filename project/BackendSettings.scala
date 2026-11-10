import sbt._
import sbt.Def.settings
import sbt.Keys.libraryDependencies

object BackendSettings {
  def apply(): Seq[Def.Setting[_]] = settings(
    libraryDependencies ++= Seq(
      "com.h2database" % "h2" % "1.4.199" % Test,
      // BCrypt library for hashing password
      "org.mindrot" % "jbcrypt" % "0.3m",
      // Database driver
      "org.postgresql" % "postgresql" % "42.2.5",
      "com.lihaoyi"   %% "os-lib"     % "0.10.4",
      "com.lihaoyi"   %% "requests"   % "0.9.0"
    )
  )

}
