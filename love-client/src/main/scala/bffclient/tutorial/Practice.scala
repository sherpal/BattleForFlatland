package bffclient.tutorial

import scala.concurrent.{ExecutionContext, Future}

import bffclient.menu.MenuApi
import gamelogic.docs.BossMetadata
import gamelogic.entities.boss.Boss101
import menus.data.User
import models.bff.outofgame.PlayerClasses
import models.bff.outofgame.gameconfig.*
import services.i18n.I18n
import utils.misc.RGBColour

/** A practice fight of the tutorial: a game against Boss 101 where we play a class, ready, and AIs
  * play the rest of the team (like a player would set it up in the lobby).
  */
object Practice {
  val bossName: String = Boss101.name

  /** Creates the game (named, or failing, in the language of `i18n`): its id, once we are in it. */
  def create(api: MenuApi, user: User, i18n: I18n)(using ExecutionContext): Future[Int] = for
    games <- api.games
    _ <-
      if games.exists(game => !game.started && game.containsPlayer(user)) then
        Future.failed(new Exception(i18n.alreadyInAGame))
      else Future.unit
    // Names are unique, and started games keep theirs
    taken = games.map(_.game.gameName).toSet
    base  = i18n.practiceGameName(user.name)
    name  = (Iterator.single(base) ++ Iterator.from(2).map(i => s"$base $i")).find(!taken(_)).get
    game <- api.createGame(name)
  yield game.id

  /** Our class, the boss, and the AIs. */
  def setUp(
      api: MenuApi,
      user: User,
      gameId: Int,
      playerClass: PlayerClasses,
      colour: RGBColour
  )(using ExecutionContext): Future[Unit] = {
    val me = PlayerInfo(
      PlayerName.HumanPlayerName(user.name),
      Some(playerClass),
      Some(colour),
      PlayerStatus.Ready,
      PlayerType.Human
    )
    val ais = BossMetadata
      .maybeMetadataByName(bossName)
      .flatMap(_.fillWithAI(Vector(playerClass)))
      .fold(0)(_.size)
    for
      _ <- api.changePlayerInfo(gameId, me)
      _ <- api.changeGameConfig(gameId, GameConfiguration.GameConfigMetadata(bossName))
      _ <- (1 to ais).foldLeft(Future.unit)((previous, _) =>
        previous.flatMap(_ => api.addAI(gameId).map(_ => ()))
      )
    yield ()
  }
}
