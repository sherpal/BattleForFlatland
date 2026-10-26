package bffclient.menu

import bffclient.love.Graphics
import bffclient.render.Sprites
import bffclient.ui.*
import gamelogic.docs.{AbilityDoc, BossMetadata}
import models.bff.outofgame.PlayerClasses

/** What the boss and the classes do, like the frontend's `GameDocs`: a tab for the boss (which the
  * game creator chooses here), a tab for the classes (ours unless another one is chosen), scrolled
  * with the mouse wheel.
  */
final class DocsPanel {
  private var tab           = "Boss"
  private var browsedClass  = Option.empty[PlayerClasses]
  private var scroll        = 0.0
  private var contentHeight = 0.0

  private def shownClass(myClass: Option[PlayerClasses]): PlayerClasses =
    browsedClass.orElse(myClass).getOrElse(PlayerClasses.Triangle)

  /** @param changeBoss
    *   when we choose the boss (only the game creator does)
    */
  def draw(
      ui: Ui,
      panel: Rect,
      bossName: String,
      changeBoss: Option[String => Unit],
      enabled: Boolean,
      myClass: Option[PlayerClasses]
  ): Unit = {
    ui.panel(panel)
    ui.segmented(
      Seq("Boss", "Classes"),
      Some(tab),
      Rect(panel.x + 12, panel.y + 10, panel.width - 24, 32)
    ).foreach { chosen =>
      tab = chosen; scroll = 0
    }
    val chooserRect = Rect(panel.x + 76, panel.y + 52, panel.width - 88, 34)
    if tab == "Boss" then {
      ui.text("Boss", panel.x + 16, panel.y + 58, colour = Theme.muted)
      changeBoss match {
        case Some(change) =>
          ui.selector(BossMetadata.allBossNames, bossName, chooserRect, enabled = enabled).foreach(change)
        case None => ui.textIn(bossName, chooserRect)
      }
    } else {
      ui.text("Class", panel.x + 16, panel.y + 58, colour = Theme.muted)
      ui.selector(PlayerClasses.allChoices, shownClass(myClass), chooserRect, (_: PlayerClasses).value)
        .foreach { cls =>
          browsedClass = Some(cls); scroll = 0
        }
    }
    val area = Rect(panel.x + 1, panel.y + 96, panel.width - 2, panel.height - 104)
    if ui.hovered(area) then
      scroll = math.max(0.0, math.min(scroll - ui.wheel * 40, contentHeight - area.height))
    ui.clipped(area) {
      val top = area.y - scroll
      val end =
        if tab == "Boss" then
          BossMetadata.maybeMetadataByName(bossName).fold(top)(metadata =>
            docs(
              ui,
              area.x + 15,
              top,
              area.width - 30,
              s"${metadata.maxLife.toInt} life points, for ${metadata.intendedFor} player(s)",
              metadata.description,
              metadata.abilitiesDoc
            )
          )
        else {
          val builder  = shownClass(myClass).builder
          val resource = builder.initialResourceAmount
          docs(
            ui,
            area.x + 15,
            top,
            area.width - 30,
            s"${builder.initialMaxLife.toInt} life points, ${resource.amount.toInt} ${resource.resourceType.toString.toLowerCase}" +
              (if myClass.contains(shownClass(myClass)) then " (your class)" else ""),
            builder.description,
            builder.abilitiesDoc
          )
        }
      contentHeight = end + scroll - area.y + 8
    }
    if contentHeight > area.height then // there is more: a scroll bar
      ui.fill(
        Rect(
          area.right - 5,
          area.y + area.height * scroll / contentHeight,
          3,
          area.height * area.height / contentHeight
        ),
        Theme.muted.withAlpha(0.6)
      )
  }

  /** Facts, description and abilities (the entity's own first, then each add's, like the
    * frontend's): returns where it ends.
    */
  private def docs(
      ui: Ui,
      x: Double,
      top: Double,
      width: Double,
      facts: String,
      description: String,
      abilities: Vector[AbilityDoc]
  ): Double = {
    var y = top
    ui.text(facts, x, y, Theme.small, Theme.muted)
    y += 22
    y += ui.paragraph(description, x, y, width, Theme.small) + 14
    val (own, adds) = abilities.partition(_.caster.isEmpty)
    val byCaster    = adds
      .groupBy(_.caster.get)
      .toVector
      .sortBy((caster, _) => adds.indexWhere(_.caster.contains(caster)))
    for (title, docs) <- ("Abilities" -> own) +: byCaster if docs.nonEmpty do {
      ui.text(title, x, y, Theme.body, Theme.text)
      y += 24
      for doc <- docs do {
        val textLeft = Sprites.abilityIcons.get(doc.abilityId) match {
          case Some(icon) =>
            Graphics.setColor(1, 1, 1)
            icon.draw(x + 12, y + 12, 0, 24, 24)
            x + 32
          case None => x
        }
        ui.text(doc.name, textLeft, y, Theme.small, Theme.primary.lighter(0.4))
        val details = Seq(
          s"Cast: ${doc.castingTimeRepr}",
          s"Cooldown: ${doc.cooldownRepr}"
        ) ++ doc.maybeCostRepr.map(c => s"Cost: $c")
        val detailsHeight = ui.paragraph(
          details.mkString("  ·  "),
          textLeft,
          y + 16,
          x + width - textLeft,
          Theme.small,
          Theme.muted
        )
        y += 18 + detailsHeight
        y += ui.paragraph(doc.description, textLeft, y, x + width - textLeft, Theme.small) + 10
      }
    }
    y
  }
}
