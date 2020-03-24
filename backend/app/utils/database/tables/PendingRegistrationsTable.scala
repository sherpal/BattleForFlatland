package utils.database.tables

import java.time.LocalDateTime

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag
import utils.database.models.PendingRegistration

final class PendingRegistrationsTable(tag: Tag) extends Table[PendingRegistration](tag, "pending_registrations") {

  def registrationKey = column[String]("registration_key", O.PrimaryKey)
  def userName        = column[String]("user_name")
  def hashedPassword  = column[String]("hashed_password")
  def mailAddress     = column[String]("mail_address")
  def createdOn       = column[LocalDateTime]("created_on")

  def * =
    (registrationKey, userName, hashedPassword, mailAddress, createdOn) <> (PendingRegistration.tupled, PendingRegistration.unapply)

}

object PendingRegistrationsTable {

  def query: TableQuery[PendingRegistrationsTable] = TableQuery[PendingRegistrationsTable]

}
