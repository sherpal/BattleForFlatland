import com.heroku.sbt.HerokuPlugin.autoImport.{
  herokuAppName,
  herokuIncludePaths,
  herokuProcessTypes,
  herokuSkipSubProjects
}
import sbt._
import sbt.Def.settings
import sbt.Keys.libraryDependencies
import play.sbt.PlayImport._

object BackendSettings {

  def apply(): Seq[Def.Setting[_]] = settings(
    libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test,
    libraryDependencies ++= Seq(evolutions),
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-slick" % "4.0.2",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.3.2",
      "com.h2database" % "h2" % "1.4.199" % Test,
      "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
      // BCrypt library for hashing password
      "org.mindrot" % "jbcrypt" % "0.3m",
      // Database driver
      "org.postgresql" % "postgresql" % "42.2.5"
    )
  )

  def herokuSettings(): Seq[Def.Setting[_]] = settings(
    herokuAppName in Compile := "full-stack-scala-example",
    herokuProcessTypes in Compile := Map(
      "web" -> "target/universal/stage/bin/backend -Dhttp.port=$PORT" // command for Heroku to launch the server
      //"worker" -> "java -jar target/universal/stage/lib/my-worker.jar"
    ),
    herokuIncludePaths in Compile := Seq(
      "backend/app",
      "backend/conf/routes",
      "backend/conf/application.conf",
      "backend/public"
    ),
    herokuSkipSubProjects in Compile := false
  )

}
