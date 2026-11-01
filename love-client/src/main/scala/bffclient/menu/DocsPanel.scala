package bffclient.menu

import bffclient.gui.*
import bffclient.render.Sprites
import bffclient.ui.{Colour, Theme}
import gamelogic.docs.{AbilityDoc, BossMetadata}
import models.bff.outofgame.PlayerClasses

/** What the boss and the classes do, like the frontend's `GameDocs`: a tab for the boss (which the
  * game creator chooses here, with `changeBoss`), a tab for the classes (ours unless another one is
  * chosen), scrolled with the mouse wheel.
  *
  * Its frame is to be anchored by its owner; [[update]] shows the game's state.
  */
final class DocsPanel(parent: Frame, changeBoss: String => Unit) {
  private enum Tab { case Boss, Classes }

  private var tab          = Tab.Boss
  private var browsedClass = Option.empty[PlayerClasses]
  private var bossName     = ""
  private var myClass      = Option.empty[PlayerClasses]
  private var bossChooser  = false

  val frame: Frame = Templates.panel(parent)

  private val tabs = Segmented[Tab](frame)
  tabs.setPoint(Point.TopLeft, 12, 10)
  tabs.setPoint(Point.TopRight, -10, 10)
  tabs.setHeight(32)
  tabs.setOptions(Tab.values.toSeq, Some(tab))
  tabs.setScript(tabs.OnValueChanged) { chosen =>
    tab = chosen
    showContents()
  }

  private val chooserLabel = frame.createFontString(colour = Theme.muted)
  chooserLabel.setPoint(Point.TopLeft, 16, 58)

  private def placeChooser(region: Region): Unit = {
    region.setPoint(Point.TopLeft, 76, 52)
    region.setPoint(Point.TopRight, -12, 52)
    region.setHeight(34)
  }

  private val bossSelector = Selector[String](frame)
  placeChooser(bossSelector)
  bossSelector.setScript(bossSelector.OnValueChanged)(changeBoss)

  /** The boss, for the players who cannot choose it. */
  private val bossText = frame.createFontString()
  placeChooser(bossText)
  bossText.setJustifyH(JustifyH.Center)

  private val classSelector = Selector[PlayerClasses](frame, _.value)
  placeChooser(classSelector)
  classSelector.setScript(classSelector.OnValueChanged) { chosen =>
    browsedClass = Some(chosen)
    showContents()
  }

  private val area = ScrollFrame(frame)
  area.setPoint(Point.TopLeft, 1, 96)
  area.setPoint(Point.BottomRight, -1, -8)
  area.setScrollStep(40)
  area.setFitsContents(true)

  /** The contents shown, and what they show. */
  private var contents = Option.empty[(Frame, Any)]

  /** @param canChangeBoss
    *   only the game creator chooses the boss
    */
  def update(
      bossName: String,
      canChangeBoss: Boolean,
      enabled: Boolean,
      myClass: Option[PlayerClasses]
  ): Unit = {
    this.bossName = bossName
    this.myClass = myClass
    bossChooser = canChangeBoss
    bossSelector.setOptions(BossMetadata.allBossNames, Some(bossName))
    bossSelector.setEnabled(enabled)
    bossText.setText(bossName)
    showContents()
  }

  private def shownClass: PlayerClasses =
    browsedClass.orElse(myClass).getOrElse(PlayerClasses.Triangle)

  /** Makes the docs again when what they show changed. */
  private def showContents(): Unit = {
    chooserLabel.setText(if tab == Tab.Boss then "Boss" else "Class")
    bossSelector.setShown(tab == Tab.Boss && bossChooser)
    bossText.setShown(tab == Tab.Boss && !bossChooser)
    classSelector.setShown(tab == Tab.Classes)
    classSelector.setOptions(PlayerClasses.allChoices, Some(shownClass))
    val key = if tab == Tab.Boss then bossName else (shownClass, myClass)
    if !contents.exists(_._2 == key) then {
      contents.foreach(_._1.remove())
      val docs = Frame(area.scrollChild) // as wide as the scroll child, and as high as nothing
      docs.setPoint(Point.TopLeft)
      docs.setPoint(Point.TopRight)
      if tab == Tab.Boss then
        BossMetadata
          .maybeMetadataByName(bossName)
          .foreach(metadata =>
            fill(
              docs,
              s"${metadata.maxLife.toInt} life points, for ${metadata.intendedFor} player(s)",
              metadata.description,
              metadata.abilitiesDoc
            )
          )
      else {
        val builder  = shownClass.builder
        val resource = builder.initialResourceAmount
        fill(
          docs,
          s"${builder.initialMaxLife.toInt} life points, ${resource.amount.toInt} ${resource.resourceType.toString.toLowerCase}" +
            (if myClass.contains(shownClass) then " (your class)" else ""),
          builder.description,
          builder.abilitiesDoc
        )
      }
      contents = Some((docs, key))
      area.setVerticalScroll(0)
    }
  }

  /** Facts, description and abilities (the entity's own first, then each add's, like the
    * frontend's), one under the other.
    */
  private def fill(
      docs: Frame,
      facts: String,
      description: String,
      abilities: Vector[AbilityDoc]
  ): Unit = {
    val margin = 15.0
    // each text is under the previous one, `indent` pixels from the left
    var previous = Option.empty[(FontString, Double)]
    def text(content: String, size: Int, colour: Colour, gap: Double, indent: Double = 0) = {
      val fontString = docs.createFontString(content, size, colour)
      fontString.setWordWrap(true)
      previous match {
        case Some((above, aboveIndent)) =>
          fontString.setPoint(Point.TopLeft, above, Point.BottomLeft, indent - aboveIndent, gap)
          fontString.setPoint(Point.TopRight, above, Point.BottomRight, 0, gap)
        case None =>
          fontString.setPoint(Point.TopLeft, docs, Point.TopLeft, margin + indent, gap)
          fontString.setPoint(Point.TopRight, docs, Point.TopRight, -margin, gap)
      }
      previous = Some((fontString, indent))
      fontString
    }

    text(facts, Theme.small, Theme.muted, 0)
    text(description, Theme.small, Theme.text, 6)
    val (own, adds) = abilities.partition(_.caster.isEmpty)
    val byCaster = adds
      .groupBy(_.caster.get)
      .toVector
      .sortBy((caster, _) => adds.indexWhere(_.caster.contains(caster)))
    for (title, docsOfCaster) <- ("Abilities" -> own) +: byCaster if docsOfCaster.nonEmpty do {
      text(title, Theme.body, Theme.text, 14)
      for doc <- docsOfCaster do {
        val icon   = Sprites.abilityIcons.get(doc.abilityId)
        val indent = if icon.isDefined then 32.0 else 0.0
        val name   = text(doc.name, Theme.small, Theme.primary.lighter(0.4), 6, indent)
        for sprite <- icon do {
          val texture = docs.createTexture()
          texture.setImage(sprite.image, sprite.quad(), sprite.frameWidth, sprite.frameHeight)
          texture.setSize(24, 24)
          texture.setPoint(Point.TopLeft, name, Point.TopLeft, -32, 0)
        }
        val details = Seq(
          s"Cast: ${doc.castingTimeRepr}",
          s"Cooldown: ${doc.cooldownRepr}"
        ) ++ doc.maybeCostRepr.map(c => s"Cost: $c")
        text(details.mkString("  ·  "), Theme.small, Theme.muted, 0, indent)
        text(doc.description, Theme.small, Theme.text, 2, indent)
      }
    }
  }
}
