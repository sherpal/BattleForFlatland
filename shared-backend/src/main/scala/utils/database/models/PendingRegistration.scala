package utils.database.models

import java.time.LocalDateTime

final case class PendingRegistration(
    registrationKey: String,
    userName: String,
    hashedPassword: String,
    mailAddress: String,
    createdOn: LocalDateTime
)
