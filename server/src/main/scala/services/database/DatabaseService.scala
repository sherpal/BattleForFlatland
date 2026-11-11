package services.database

import org.flywaydb.core.Flyway

import java.nio.file.{Files, Path}
import scalasql.simple.*

class DatabaseService(dataDirectory: Path, inTest: Boolean = false) {

  import SqliteDialect.*

  Files.createDirectories(dataDirectory)

  private val sqliteDataSource = org.sqlite.SQLiteDataSource()
  sqliteDataSource.setUrl(
    s"jdbc:sqlite:${dataDirectory.resolve("database.db")}"
  )

  // noinspection TypeAnnotation
  val client = scalasql.DbClient.DataSource(
    sqliteDataSource,
    config = new scalasql.Config:
      //      override def logSql(sql: String, file: String, line: Int) = {
      //        if sql.toLowerCase.contains("case") then println(s"query: $sql")
      //      }

      override def nameMapper(v: String) = Config.camelToSnake(v)

      override def columnNameMapper(v: String): String = Config.camelToSnake(v)
  )

  lazy val db = client.getAutoCommitClientConnection

  private val flyway =
    Flyway
      .configure()
      .loggers(if inTest then "slf4j" else "auto")
      .dataSource(sqliteDataSource)
      .load()
  flyway.migrate()

}
