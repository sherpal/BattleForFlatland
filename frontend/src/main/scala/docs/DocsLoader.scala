package docs

import gamelogic.entities.boss.Boss101

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DocsLoader {

  object markdown {
    @js.native @JSImport("resources/docs/TEST.md", JSImport.Default)
    object Test extends js.Object

    object bosses {
      @js.native @JSImport("resources/docs/bosses/Boss101.md", JSImport.Default)
      object Boss101Description extends js.Object
    }
  }

  markdown.Test
  markdown.bosses.Boss101Description

  def bossDescription(name: String): Option[String] =
    Map(
      Boss101.name -> markdown.bosses.Boss101Description
    ).get(name).map(_.asInstanceOf[String])

}
