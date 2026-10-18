package components.beforegame

import assets.Asset
import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.{
  AvatarColorScheme,
  AvatarInitials,
  AvatarShape,
  AvatarSize,
  TagDesign,
  TitleLevel
}
import com.raquo.laminar.api.L.*
import gamelogic.docs.{formatNumber, AbilityDoc, BossMetadata}
import models.bff.outofgame.PlayerClasses

/** Displays, in the lobby, the documentation of the boss and of the classes that players can
  * choose, so that they know what they are up against and what they can do.
  */
object GameDocs {

  /** Panel describing the boss with the given name. */
  def bossDocs(bossName: String): HtmlElement =
    Panel(
      _.headerText  := s"Boss: $bossName",
      _.headerLevel := TitleLevel.H3,
      BossMetadata.maybeMetadataByName(bossName) match {
        case None => Text(s"There is no information about $bossName.")
        case Some(boss) =>
          div(
            facts(
              s"${formatNumber(boss.maxLife)} life points",
              s"Intended for ${boss.intendedFor} players"
            ),
            Text(boss.description),
            abilitiesSection(boss.abilitiesDoc)
          )
      }
    )

  /** Panel with one tab per class. The tab of `maybeSelectedClass` is selected, if any. */
  def classesDocs(maybeSelectedClass: Option[PlayerClasses]): HtmlElement =
    Panel(
      _.headerText  := maybeSelectedClass.fold("Classes")(cls => s"Your class: ${cls.value}"),
      _.headerLevel := TitleLevel.H3,
      Option.when(maybeSelectedClass.isEmpty)(
        Text("Choose a class above. Here is what each of them can do.")
      ),
      TabContainer(
        _.collapsed := false,
        PlayerClasses.allChoices.map(cls =>
          TabContainer.tab(
            _.text     := cls.value,
            _.selected := maybeSelectedClass.contains(cls),
            classDocs(cls)
          )
        )
      )
    )

  private def classDocs(cls: PlayerClasses): HtmlElement = {
    val builder  = cls.builder
    val resource = builder.initialResourceAmount
    div(
      // contains the margins of its children, which would otherwise overflow the tab container
      display := "flow-root",
      facts(
        s"${formatNumber(builder.initialMaxLife)} life points",
        s"${formatNumber(resource.amount)} ${resource.resourceType.toString.toLowerCase}"
      ),
      Text(builder.description),
      abilitiesSection(builder.abilitiesDoc)
    )
  }

  private def facts(items: String*): HtmlElement =
    div(
      display.flex,
      flexWrap.wrap,
      styleProp("gap") := "0.5em",
      margin           := "0.5em 0",
      items.map(item => Tag(_.design := TagDesign.Information, _.hideStateIcon := true, item))
    )

  /** Abilities of the entity itself come first, then the ones of each of its adds. */
  private def abilitiesSection(abilities: Vector[AbilityDoc]): HtmlElement = {
    val (ownAbilities, addsAbilities) = abilities.partition(_.caster.isEmpty)
    val addsAbilitiesByCaster = addsAbilities
      .groupBy(_.caster.get)
      .toVector
      .sortBy((caster, _) => addsAbilities.indexWhere(_.caster.contains(caster)))
    div(
      Title.h4("Abilities", marginTop := "1em"),
      ownAbilities.map(abilityItem),
      addsAbilitiesByCaster.map((caster, casterAbilities) =>
        div(Title.h5(caster, marginTop := "1em"), casterAbilities.map(abilityItem))
      )
    )
  }

  private def abilityItem(ability: AbilityDoc): HtmlElement =
    div(
      display.flex,
      styleProp("gap") := "0.8em",
      margin           := "0.6em 0",
      abilityIcon(ability),
      div(
        flex := "1",
        div(
          display.flex,
          flexWrap.wrap,
          alignItems.center,
          styleProp("gap") := "0.4em",
          Label(ability.name, fontWeight.bold, marginRight := "0.3em"),
          infoTag(s"Cast: ${ability.castingTimeRepr}"),
          infoTag(s"Cooldown: ${ability.cooldownRepr}"),
          ability.maybeCostRepr.map(cost => infoTag(s"Cost: $cost"))
        ),
        Text(ability.description, display.block, marginTop := "0.2em")
      )
    )

  private def infoTag(text: String): HtmlElement =
    Tag(_.design := TagDesign.Neutral, _.hideStateIcon := true, text)

  /** Accent colour schemes of the theme, from which ability avatars without an icon pick theirs. */
  private val accentColourSchemes: Vector[AvatarColorScheme] = Vector(
    AvatarColorScheme.Accent1,
    AvatarColorScheme.Accent2,
    AvatarColorScheme.Accent3,
    AvatarColorScheme.Accent4,
    AvatarColorScheme.Accent5,
    AvatarColorScheme.Accent6,
    AvatarColorScheme.Accent7,
    AvatarColorScheme.Accent8,
    AvatarColorScheme.Accent9,
    AvatarColorScheme.Accent10
  )

  /** The in-game icon of the ability if it has one, otherwise its initial. */
  private def abilityIcon(ability: AbilityDoc): HtmlElement =
    Avatar(
      _.size           := AvatarSize.XS,
      _.shape          := AvatarShape.Square,
      _.accessibleName := ability.name,
      flexShrink       := 0,
      Asset.abilityAssetMap.get(ability.abilityId) match {
        case Some(asset) => img(src := asset.pathStr, alt := ability.name)
        case None =>
          Seq(
            Avatar.initials := AvatarInitials.One(ability.name.head),
            Avatar.colorScheme := accentColourSchemes(
              ability.abilityId.value % accentColourSchemes.length
            )
          )
      }: Modifier[HtmlElement]
    )

}
