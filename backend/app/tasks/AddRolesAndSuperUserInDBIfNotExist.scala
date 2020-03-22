package tasks

import akka.actor.ActorSystem
import javax.inject.Inject
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import services.config._
import services.crypto.Crypto
import services.database.db.Database.dbProvider
import services.database.users._
import services.logging._
import slick.jdbc.JdbcProfile
import zio.Runtime
import zio.clock._

import scala.concurrent.ExecutionContext

/**
  * This task will be launched at the start of play to add the admin user if it is not yet in the database.
  */
final class AddRolesAndSuperUserInDBIfNotExist @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
    actorSystem: ActorSystem
)(implicit executionContext: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] {

  lazy val logger: Logger = Logger("AddAdminInDB")

  private val layer = (dbProvider(db) >>> Users.live) ++ Clock.live ++ Configuration.live ++ Crypto.live ++
    PlayLogging.live(logger)

  Runtime.default.unsafeRun(TasksPrograms.addingRolesAndSuperUser.provideLayer(layer))

}
