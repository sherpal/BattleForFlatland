package services.database.db

import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.{JdbcBackend, JdbcProfile}
import zio._

object Database {

  trait Service {
    def db: UIO[JdbcProfile#Backend#Database]
  }

  type DBProvider = Has[Database.Service]

  def runAsTask[R](a: DBIOAction[R, NoStream, Nothing])(implicit db: JdbcProfile#Backend#Database): Task[R] =
    ZIO.fromFuture { implicit ec =>
      db.run(a)
    }

  def run[R](a: DBIOAction[R, NoStream, Nothing]): ZIO[DBProvider, Throwable, R] =
    for {
      database <- db
      result <- ZIO.fromFuture { implicit ec =>
        database.run(a)
      }
    } yield result

  def dbProvider(database: JdbcProfile#Backend#Database): Layer[Nothing, Has[Service]] =
    ZLayer.succeed(new Service {
      def db: UIO[JdbcBackend#DatabaseDef] = ZIO.succeed(database)
    })

  def db: ZIO[DBProvider, Nothing, JdbcProfile#Backend#Database] = ZIO.accessM(_.get[Service].db)

}
