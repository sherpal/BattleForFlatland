package programs.frontend.gamedocs

import services.http._
import urldsl.errors.DummyError
import urldsl.language.PathSegment
import urldsl.language.PathSegment.dummyErrorImpl._
import urldsl.language.QueryParameters.dummyErrorImpl._
import zio.ZIO

val basePath: PathSegment[Unit, DummyError] = root / "docs" / "game-docs"

def bossDescription(maybeBossName: Option[String]) = maybeBossName match {
  case Some(bossName) =>
    for {
      query       <- ZIO.succeed(param[String]("bossName"))
      path        <- ZIO.succeed(basePath / "boss-description")
      description <- getOrElse[String, String](path, query)(bossName, s"No description found ??? for $bossName")
    } yield description
  case None => ZIO.succeed("No boss selected")
}
