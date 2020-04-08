package utils.database.models

import java.time.LocalDateTime

final case class UserInGameTable(gameId: String, userId: String, joinedOn: LocalDateTime)

object UserInGameTable {

  def now(gameId: String, userId: String): UserInGameTable = UserInGameTable(gameId, userId, LocalDateTime.now)

  final def tupled: ((String, String, LocalDateTime)) => UserInGameTable = (apply _).tupled

}
