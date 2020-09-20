package programs.frontend

import services.http._
import urldsl.errors.DummyError
import urldsl.language.PathSegment
import urldsl.language.PathSegment.dummyErrorImpl._
import urldsl.language.QueryParameters.dummyErrorImpl._
import zio.UIO

package object gamedocs {

  val basePath: PathSegment[Unit, DummyError] = root / "docs" / "game-docs"

  def bossDescription(maybeBossName: Option[String]) = maybeBossName match {
    case Some(bossName) =>
      for {
        query       <- UIO(param[String]("bossName"))
        path        <- UIO(basePath / "boss-description")
        description <- getOrElse[String, String](path, query)(bossName, s"No description found ??? for $bossName")
      } yield description
    case None => UIO("No boss selected")
  }

}
