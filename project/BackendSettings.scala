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
  val zioVersion = "1.0.0-RC18-2"

  def playSpecifics(): Seq[Def.Setting[_]] = settings(
    libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test,
    libraryDependencies ++= Seq(evolutions),
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0"
    )
  )

  def testsDeps(): Seq[Def.Setting[_]] = settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-test" % zioVersion % "test",
      "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
      "dev.zio" %% "zio-test-magnolia" % zioVersion % "test" // optional
    )
  )

  def apply(): Seq[Def.Setting[_]] = settings(
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-slick" % "4.0.2",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.3.2",
      "com.h2database" % "h2" % "1.4.199" % Test,
      // BCrypt library for hashing password
      "org.mindrot" % "jbcrypt" % "0.3m",
      // Database driver
      "org.postgresql" % "postgresql" % "42.2.5",
      "org.webjars" % "swagger-ui" % "2.2.0",
      "com.typesafe.akka" %% "akka-actor-typed" % "2.6.4",
      "com.typesafe.akka" %% "akka-stream-typed" % "2.6.4",
      "com.typesafe.akka" %% "akka-http" % "10.1.11"
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
