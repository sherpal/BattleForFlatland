package services.menugames

import errors.ErrorADT
import eventsourcing.{CastorScheduler, EventSourcingService, SqlEventStore}
import menus.data.User
import services.database.DatabaseService

final class MenuGamesTest extends munit.FunSuite {

  given castor.Context = castor.Context.Simple.global

  private val alice = User("alice")
  private val bob   = User("bob")
  private val carol = User("carol")

  /** What the server does when it starts, on the database of `folder`. */
  private def menuGames(folder: os.Path): MenuGames = {
    val database = DatabaseService(folder.toNIO)
    MenuGames(
      EventSourcingService(
        EventSourcingService.Config.default,
        SqlEventStore(database.client),
        CastorScheduler()
      ),
      database
    )
  }

  test("games get increasing ids, and the open ones are loaded again after a restart") {
    val folder = os.temp.dir()
    val before = menuGames(folder)

    val aliceGame = before.createGame("alice's", None, alice).toOption.get
    val bobGame   = before.createGame("bob's", None, bob).toOption.get
    assertEquals((aliceGame.id, bobGame.id), (1, 2))

    assert(before.joinGame(carol, aliceGame.id, None).isRight)
    assertEquals(
      before.joinGame(carol, bobGame.id, None),
      Left(ErrorADT.UserAlreadyPlaying("carol"))
    )
    assertEquals(
      before.createGame("alice's", None, User("dave")),
      Left(ErrorADT.GameExists("alice's"))
    )
    val credentials = before.launchGame(alice, aliceGame.id).toOption.get
    assertEquals(
      before.launchGame(alice, aliceGame.id),
      Left(ErrorADT.GameAlreadyLaunched(aliceGame.id))
    )
    before.deleteGame(bobGame.id)
    assertEquals(
      before.menuGames.map(game => (game.id, game.players, game.started)),
      Vector((1, Vector(alice, carol), true))
    )

    val after = menuGames(folder)
    assertEquals(after.menuGames, before.menuGames)
    assertEquals(after.gameCredentials(aliceGame.id), Some(credentials))
    assertEquals(
      after.retrieveAllGameCredentials(aliceGame.id, credentials.secret).map(_.gameInfo.id),
      Right(1)
    )
    assertEquals(after.joinGame(bob, bobGame.id, None), Left(ErrorADT.GameDoesNotExist(bobGame.id)))
    assertEquals(after.createGame("bob's", None, bob).map(_.id), Right(3))
  }

}
