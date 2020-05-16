package utils.database.tables

import java.time.LocalDateTime

import models.bff.outofgame.DBMenuGame
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

final class MenuGamesTable(tag: Tag) extends Table[DBMenuGame](tag, "game_tables") {

  def gameId                    = column[String]("game_id", O.PrimaryKey)
  def gameName                  = column[String]("game_name")
  def hashedPassword            = column[Option[String]]("game_hashed_password")
  def creatorId                 = column[String]("game_creator_id")
  def createdOn                 = column[LocalDateTime]("created_on")
  def gameConfigurationAsString = column[String]("game_config_str", O.Length(255 * 255))

  def * =
    (gameId, gameName, hashedPassword, creatorId, createdOn, gameConfigurationAsString) <> (DBMenuGame.tupled, DBMenuGame.unapply)

  def creator = foreignKey("creator_fk", creatorId, UsersTable.query)(
    _.userId,
    onDelete = ForeignKeyAction.Cascade
  )

}

object MenuGamesTable {

  def query: TableQuery[MenuGamesTable] = TableQuery[MenuGamesTable]

}
