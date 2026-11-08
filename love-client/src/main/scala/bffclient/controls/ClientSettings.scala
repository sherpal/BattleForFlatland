package bffclient.controls

import bffclient.love.Filesystem
import gamelogic.abilities.Ability
import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import io.circe.parser.decode
import io.circe.syntax.*
import models.bff.outofgame.PlayerClasses
import services.i18n.{EnglishI18n, I18n, Language}

/** The client's own settings (BFF's [[models.bff.ingame.Controls]] cannot hold them), stored as
  * JSON in LÖVE's save directory and changed in the settings (see [[SettingsEditor]]).
  *
  * @param hudScale
  *   the size of the HUD (frames, action bar, ...), whatever the window's size
  * @param hostGameServers
  *   whether the player's computer may run the game server of their games (see
  *   `hosting.HostingAgent`). Without it, the client downloads nothing to run them, e.g. for the
  *   players on a mobile network.
  * @param volumes
  *   how loud the sounds are
  * @param language
  *   the language of the texts, once chosen (on the first launch, see `menu.LanguageScreen`)
  * @param abilityOrders
  *   the order of the abilities in the action bar (and thus their keys), for the classes whose
  *   order the player changed (see [[abilityOrder]])
  */
final case class ClientSettings(
    hudScale: Double,
    hostGameServers: Boolean,
    volumes: Volumes,
    language: Option[Language],
    abilityOrders: Map[PlayerClasses, Vector[Ability.AbilityId]]
) {

  /** The texts, in English until a language is chosen. */
  def i18n: I18n = language.fold(EnglishI18n)(_.i18n)

  // asked for every frame by the action bar: computed once
  private lazy val orders: Map[PlayerClasses, Vector[Ability.AbilityId]] =
    PlayerClasses.allChoices.map { cls =>
      val abilities = cls.builder.abilities.toVector
      val chosen    = abilityOrders.getOrElse(cls, Vector.empty).filter(abilities.contains).distinct
      // the abilities not in the chosen order (e.g. added to the class since) at the end
      cls -> (chosen ++ abilities.filterNot(chosen.contains))
    }.toMap

  /** All the abilities of the class, in the order of the action bar: the `n`-th one is on the key
    * of "Ability n" in the controls.
    */
  def abilityOrder(cls: PlayerClasses): Vector[Ability.AbilityId] = orders(cls)

  /** With the abilities of the class in that order (the default one for the default order). */
  def withAbilityOrder(cls: PlayerClasses, order: Vector[Ability.AbilityId]): ClientSettings =
    copy(abilityOrders =
      if order == cls.builder.abilities.toVector then abilityOrders - cls
      else abilityOrders.updated(cls, order)
    )

  /** Writes the settings (the HUD scale is used by the next game). An error message if it failed.
    */
  def save(): Option[String] = {
    ClientSettings.known = Some(this)
    val written = Filesystem.write(ClientSettings.fileName, this.asJson.spaces2)
    Option.unless(written.success)(
      s"Could not write ${Filesystem.getSaveDirectory()}/${ClientSettings.fileName}: ${written.message}"
    )
  }
}

object ClientSettings {
  val fileName = "settings.json"

  val default = ClientSettings(
    hudScale = 1.0,
    hostGameServers = true,
    volumes = Volumes.default,
    language = None,
    abilityOrders = Map.empty
  )

  /** From 0.5 to 2. */
  val hudScales: Seq[Double] = (5 to 20).map(_ / 10.0)

  // by its code (an unknown one is no language)
  given Encoder[Language] = Encoder.encodeString.contramap(_.code)
  given Decoder[Language] =
    Decoder.decodeString.emap(code => Language.byCode(code).toRight(s"Unknown language $code"))

  // by its name
  given KeyEncoder[PlayerClasses] = KeyEncoder.encodeKeyString.contramap(_.value)
  given KeyDecoder[PlayerClasses] = KeyDecoder.instance(PlayerClasses.playerClassByName)

  given Encoder[ClientSettings] = io.circe.generic.semiauto.deriveEncoder

  // the settings written before one of them existed have its default
  given Decoder[ClientSettings] = Decoder.instance(cursor =>
    for
      hudScale        <- cursor.getOrElse[Double]("hudScale")(default.hudScale)
      hostGameServers <- cursor.getOrElse[Boolean]("hostGameServers")(default.hostGameServers)
      volumes         <- cursor.getOrElse[Volumes]("volumes")(default.volumes)
      language = cursor.get[Language]("language").toOption
      abilityOrders = cursor
        .get[Map[PlayerClasses, Vector[Ability.AbilityId]]]("abilityOrders")
        .getOrElse(default.abilityOrders)
    yield ClientSettings(hudScale, hostGameServers, volumes, language, abilityOrders)
  )

  private var known = Option.empty[ClientSettings]

  /** The settings as they are now: the last ones saved, otherwise the ones of the file (read once).
    */
  def current: ClientSettings = known.getOrElse {
    val loaded = load()
    known = Some(loaded)
    loaded
  }

  def load(): ClientSettings =
    Filesystem.getInfo(fileName).fold(default) { _ =>
      decode[ClientSettings](Filesystem.read(fileName).contents).toOption
        .filter(settings => hudScales.contains(settings.hudScale) && settings.volumes.valid)
        .getOrElse(default)
    }
}

/** The volumes of the sounds, each a level from 0 (off) to 10 (full): the main one, and one per
  * kind of sound (only the abilities' for now), on top of the main one.
  */
final case class Volumes(main: Int, abilities: Int) {

  /** Of the sounds of the abilities, between 0 and 1. */
  def ofAbilities: Double = Volumes.fraction(main) * Volumes.fraction(abilities)

  def valid: Boolean = Seq(main, abilities).forall(Volumes.levels.contains)
}

object Volumes {
  val levels: Seq[Int] = 0 to 10

  val default = Volumes(main = 10, abilities = 10)

  private def fraction(level: Int): Double = level / levels.max.toDouble

  given Encoder[Volumes] = io.circe.generic.semiauto.deriveEncoder

  // like the settings, a volume added later has its default
  given Decoder[Volumes] = Decoder.instance(cursor =>
    for
      main      <- cursor.getOrElse[Int]("main")(default.main)
      abilities <- cursor.getOrElse[Int]("abilities")(default.abilities)
    yield Volumes(main, abilities)
  )
}
