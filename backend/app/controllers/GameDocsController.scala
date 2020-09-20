package controllers

import errors.ErrorADT.RawNotFound
import gamelogic.entities.boss.Boss101
import gamelogic.entities.boss.dawnoftime.{Boss102, Boss103}
import io.circe.Encoder
import javax.inject.Inject
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.WriteableImplicits._
import views.txt.gamedocs._

final class GameDocsController @Inject()(
    cc: ControllerComponents
) extends AbstractController(cc) {

  private def sendDocs(docMarkdown: play.twirl.api.TxtFormat.Appendable) =
    Ok(Encoder[String].apply(docMarkdown.toString))

  def bossDocs(bossName: String): Action[AnyContent] = Action {
    bossName match {
      case Boss101.name => sendDocs(bossdescriptions.boss101.render())
      case Boss102.name => sendDocs(bossdescriptions.boss102.render())
      case Boss103.name => sendDocs(bossdescriptions.boss103.render())
      case _            => NotFound(RawNotFound().json)
    }
  }

}
